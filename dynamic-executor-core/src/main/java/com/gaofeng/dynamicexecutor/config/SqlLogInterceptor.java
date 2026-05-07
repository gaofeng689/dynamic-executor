package com.gaofeng.dynamicexecutor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MyBatis SQL 日志拦截器。拦截 StatementHandler，将 SQL 与参数拼接为完整可执行语句后打印。
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "update", args = {java.sql.Statement.class}),
        @Signature(type = StatementHandler.class, method = "query", args = {java.sql.Statement.class, org.apache.ibatis.session.ResultHandler.class})
})
public class SqlLogInterceptor implements Interceptor {

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\?");
    // 线程安全的日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            try {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = boundSql.getSql();
                Object parameter = boundSql.getParameterObject();
                List<ParameterMapping> mappings = boundSql.getParameterMappings();

                String fullSql = buildFullSql(sql, parameter, mappings);
                String method = invocation.getMethod().getName();
                log.info("\n=== SQL [{}, {}ms] ===\n{}\n====================", method, elapsed, fullSql);
            } catch (Exception e) {
                log.warn("SQL 日志打印失败: {}", e.getMessage());
            }
        }
        return result;
    }

    /**
     * 将带 ? 占位符的 SQL 与参数拼接为完整语句
     *
     * @param sql       原始 SQL（带 ?）
     * @param parameter 参数对象（可为 null）
     * @param mappings  参数映射列表
     * @return 拼接后的完整 SQL
     */
    private String buildFullSql(String sql, Object parameter, List<ParameterMapping> mappings) {
        if (parameter == null || mappings.isEmpty()) {
            return sql;
        }

        try {
            // 简单类型参数：直接替换第一个 ?（通常只有一个占位符）
            if (parameter instanceof Number || parameter instanceof String
                    || parameter instanceof Date || parameter instanceof Boolean) {
                // 注意：replaceFirst 的 replacement 需要转义特殊字符
                return sql.replaceFirst("\\?", Matcher.quoteReplacement(formatParam(parameter)));
            }

            // 复杂类型（Map、POJO 等）：通过 MetaObject 获取每个属性值
            MetaObject metaObject = SystemMetaObject.forObject(parameter);
            Matcher matcher = PARAM_PATTERN.matcher(sql);
            StringBuffer sb = new StringBuffer();

            for (ParameterMapping mapping : mappings) {
                if (matcher.find()) {
                    String property = mapping.getProperty();
                    Object value = metaObject.getValue(property);
                    // 替换值并转义特殊字符（$ 和 \）
                    String replacement = Matcher.quoteReplacement(formatParam(value));
                    matcher.appendReplacement(sb, replacement);
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            // 降级输出：任何解析异常都输出原始 SQL 和参数对象
            return sql + "  [参数: " + parameter + "]";
        }
    }

    /**
     * 将参数值格式化为 SQL 字面量
     *
     * @param value 参数值
     * @return SQL 中可使用的字面量字符串
     */
    private String formatParam(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            // 单引号转义
            return "'" + ((String) value).replace("'", "''") + "'";
        }
        if (value instanceof Date) {
            // 使用线程安全的 DateTimeFormatter
            String formatted = ((Date) value).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
            return "'" + formatted + "'";
        }
        // 数字、布尔等直接输出
        return value.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可在此处读取配置参数，如是否启用、自定义日期格式等
    }
}
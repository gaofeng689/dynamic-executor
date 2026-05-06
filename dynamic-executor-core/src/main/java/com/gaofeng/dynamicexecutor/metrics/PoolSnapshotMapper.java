package com.gaofeng.dynamicexecutor.metrics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Mapper，继承 BaseMapper 获得 CRUD 能力
 */
@Mapper
public interface PoolSnapshotMapper extends BaseMapper<PoolSnapshot> {

    /** 查询最近 N 分钟的指标快照，按时间升序 */
    @Select("SELECT * FROM de_pool_snapshot WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY create_time ASC")
    List<PoolSnapshot> selectRecent(@Param("minutes") int minutes);

    /** 删除 N 小时前的过期数据 */
    @Delete("DELETE FROM de_pool_snapshot WHERE create_time < DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    int deleteOlderThan(@Param("hours") int hours);
}

CREATE DATABASE IF NOT EXISTS dynamic_executor DEFAULT CHARSET utf8mb4;

CREATE TABLE IF NOT EXISTS dynamic_executor.de_pool_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    core_pool_size INT,
    maximum_pool_size INT,
    pool_size INT,
    active_count INT,
    completed_task_count BIGINT,
    task_count BIGINT,
    queue_size INT,
    queue_remaining_capacity INT,
    queue_type VARCHAR(50),
    rejected_handler VARCHAR(50),
    keep_alive_time BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_create_time (create_time)
);

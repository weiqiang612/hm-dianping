package com.hmdp.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/5 18:19
 */

/**
 * 存储热点key，逻辑不过期
 * 包装类
 */
@Data
public class RedisData {
    private LocalDateTime expiredTime; // 逻辑过期时间
    private Object data; // 存放真正的业务数据，如shop对象
}

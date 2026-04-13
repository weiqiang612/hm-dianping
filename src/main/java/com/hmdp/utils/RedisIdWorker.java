package com.hmdp.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 *
 * 全局ID生成器
 *
 * @author weiqiang
 * @date 2026/04/06
 */

@Component
public class RedisIdWorker {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 开始时间戳，20050322距今秒数
     */
    private static final Long BEGIN_TIME = 1111449600L;

    /**
     * 32位序列号
     */
    private static final int COUNT_BITS = 32;

    /**
     *
     * 生成全局ID
     *
     * @param keyPrefix 业务前缀
     * @return {@link Long }
     */
    public Long generateId(String keyPrefix) {
        StringBuilder id = new StringBuilder();
        // 1. 第一位为符号位永远为0
        id.append(0);
        // 2. 第2到第32位为时间戳，这里我们选择距离2005年3月22日的秒数
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        Long timeStamp = now - BEGIN_TIME;
        id.append(StringUtils.leftPad(String.valueOf(timeStamp), 31, "0"));
        // 3. 序列号，从0开始
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long serialNumber = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        return timeStamp << COUNT_BITS | serialNumber;
    }


}


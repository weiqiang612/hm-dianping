package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author weiqiang
 * @version 1.0
 * @Date 2026/4/8 20:39
 */


public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString().replace("-", "") + "-";
    // 泛型为Lua脚本返回值类型
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("scripts/unlock.lua"));
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程ID作值
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 防止自动拆箱返回NULL报NPE
        return Boolean.TRUE.equals(success);
    }


    @Override
    public void unlock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 基于Lua脚本编写
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                threadId
                );

//        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        // 确实是自己的锁
//        if ((threadId).equals(value)) {
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
    }

//    @Override
//    public void unlock() {
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//
//        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        // 确实是自己的锁
//        if ((threadId).equals(value)) {
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}

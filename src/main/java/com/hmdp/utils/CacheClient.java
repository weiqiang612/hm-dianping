package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 基于StringRedisTemplate封装一个缓存工具类，满足下列需求：
 * 方法1：将任意Java对象序列化为json并存储到String类型的Key中，可以设置TTL
 * 方法2：将任意Java对象序列化为json并存储到String类型的Key中，可以设置逻辑TTL，处理缓存击穿问题
 * 方法3：根据指定的Key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
 * 方法4：根据指定的Key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
 *
 * @author weiqiang
 * @date 2026/04/06
 */

@Component
public class CacheClient {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 全局线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR =
            Executors.newFixedThreadPool(10);

    /**
     * 将任意Java对象序列化为json并存储到String类型的Key中，可以设置TTL
     *
     * @param obj  可以为null,将缓存"NULL",用来解决缓存穿透问题
     * @param key
     * @param ttl
     * @param unit
     */
    public void setWithTTL(Object obj, String key, Long ttl, TimeUnit unit) {
        // 1. 缓存穿透
        if (obj == null) {
            stringRedisTemplate.opsForValue().set(key, "NULL", ttl, unit);
            return;
        }
        // 2. 正常存值
        String jsonBean = JsonUtils.toJsonStr(obj);
        stringRedisTemplate.opsForValue().set(key, jsonBean, ttl, unit);
    }

    /**
     *
     * 根据指定的Key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     * @param keyPrefix
     * @param id
     * @param clazz
     * @param dbFallback 数据库回调函数
     * @param ttl
     * @param unit
     * @return {@link T }
     */
    public <T,ID> T getBeanWithCachePenetration(String keyPrefix,ID id, Class<T> clazz, Function<ID, T> dbFallback,Long ttl, TimeUnit unit) {
        String key = keyPrefix + id;
        String jsonBean = stringRedisTemplate.opsForValue().get(key);
        // 1. 有记录
        if (StrUtil.isNotBlank(jsonBean)) {
            // 1.1 解决缓存穿透问题
            if ("NULL".equals(jsonBean)) {
                return null;
            }
            // 1.2 反序列化为指定类型的对象返回
            return JsonUtils.toBean(jsonBean, clazz);
        }

        if (jsonBean != null) { // 只要不是 null，说明 Redis 命中了（包括存入的空值）
            if ("NULL".equals(jsonBean)) return null;
            return JsonUtils.toBean(jsonBean, clazz);
        }

        // 2. 无记录查询数据库
        T result = dbFallback.apply(id);
        // 3. 数据不存在，存"NULL"
        if (result == null) {
            setWithTTL(null,key,ttl,unit);
            return null;
        }
        // 4. 数据存在，写入Redis，并返回
        setWithTTL(result,key,ttl,unit);
        return result;
    }

    /**
     * 将任意Java对象序列化为json并存储到String类型的Key中，可以设置逻辑TTL，处理缓存击穿问题
     *
     * @param obj        不能为NULL，不能为不存在的数据设置逻辑过期时间
     * @param key
     * @param expireTime 经过该时间后，逻辑Key过期
     * @param unit
     */
    public void setWithLogicalExpire(@NonNull Object obj, String key, Long expireTime, TimeUnit unit) {
        // 1. 封装为RedisData
        RedisData redisData = new RedisData();
        redisData.setData(obj);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));
        // 2. 写入Redis
        stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(redisData));
    }

    /**
     * 根据指定的Key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
     * 需要配合缓存预热使用
     *
     * @param keyPrefix
     * @param id
     * @param clazz      最终返回对象的类型
     * @param expireTime 重建缓存指定的逻辑过期时长
     * @param unit
     * @param lockKey
     * @return {@link T }
     */
    public <T,ID> T getWithLogicalExpire(String keyPrefix,ID id ,Class<T> clazz, Long expireTime, TimeUnit unit, String lockKey,Function<ID,T> dbFallback) {
        // 1. 查询Redis
        String key = keyPrefix + id;
        String jsonBean = stringRedisTemplate.opsForValue().get(key);
        // 2. 未命中返回null(几乎不考虑这种情况，一般使用该方法时都会做缓存预热处理)
        if (StrUtil.isBlank(jsonBean)) {
            return null;
        }
        // 3. 命中查过期时间
        RedisData redisData = JsonUtils.toBean(jsonBean, RedisData.class);
        // redisData 理论上不为null
        assert redisData != null;
        LocalDateTime time = redisData.getExpireTime();
        // 4. 未过期，直接返回数据
        if (LocalDateTime.now().isBefore(time)) {
            return JsonUtils.convert(redisData.getData(), clazz);
        }
        // 5. 已过期
        // 6. 重建缓存
        // 6.1 抢锁
        if (lock(lockKey)) {
            // 6.1.1 抢到锁，另开线程做 double-check，查看数据是否真的过期了
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    String latestJson = stringRedisTemplate.opsForValue().get(key);
                    RedisData latestData = JsonUtils.toBean(latestJson, RedisData.class);
                    if (latestData == null || LocalDateTime.now().isBefore(latestData.getExpireTime())) {
                        return;
                    }
                    // 6.1.2 查询数据库，重新写入
                    T res = dbFallback.apply(id);
                    if (res == null) {
                        // 如果数据库中也不存在这个热点key，直接删除即可
                        // 这样再来访问该key，该方法返回null给上层
                        stringRedisTemplate.delete(key);
                        return;
                    }
                    setWithLogicalExpire(res,key,expireTime,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 6.1.3 释放锁
                    unlock(lockKey);
                }
            });

        }
        // 6.2 不管有没有抢到锁都返回旧数据
        return JsonUtils.convert(redisData.getData(), clazz);
    }

    /**
     * 设置锁，返回是否成功持有锁
     *
     * @return {@link Boolean }
     */
    private Boolean lock(String key) {
        Boolean haveLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(haveLock);
    }

    /**
     * 释放锁
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


}

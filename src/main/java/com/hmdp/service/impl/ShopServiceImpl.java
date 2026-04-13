package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.JsonUtils;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 声明一个全局静态线程池（避免重复创建线程）
    private static final ExecutorService CACHE_REBUILD_EXECUTOR =
            Executors.newFixedThreadPool(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

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

    /**
     *
     * 查询Redis中数据，考虑到了缓存穿透情况
     *
     * @param id
     * @return {@link Shop }
     */
    public Result queryByIdWithPenetration(Long id) {
        Shop shop = cacheClient.getBeanWithCachePenetration(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 无记录
        if (Objects.isNull(shop)) {
            return Result.fail("店铺不存在！");
        }
        // 有记录
        return Result.ok(shop);
    }

    /**
     * 将数据及其逻辑过期时间封装后写入 Redis
     *
     * @param key        Redis Key
     * @param obj        要缓存的对象
     * @param expireTime 过期时间长度
     * @param unit       时间单位
     */
    public void setWithLogicalExpire(Object obj, String key, Long expireTime, TimeUnit unit) {
        if (obj == null) {
            log.warn("Attempting to set null object for logical expire, key: {}", key);
            return;
        }

        // 1. 封装逻辑过期包装类
        RedisData redisData = new RedisData();
        redisData.setData(obj); // RedisData 的 data 属性本身就是 Object，直接存即可

        // 2. 设置逻辑过期时间
        // 将当前时间加上指定步长，转换为 LocalDateTime
        LocalDateTime logicalExpireTime = LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime));
        redisData.setExpiredTime(logicalExpireTime);

        // 3. 写入 Redis
        stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(redisData));
    }

    /**
     * 逻辑不过期
     * 1000线程 ramp-up 1秒 循环次数300， 测试吞吐量 19787 ，平均响应时间 45ms ，异常 0%
     *
     * @param id
     * @return {@link Result }
     */
    @Override
    public Result queryById(Long id) {

        Shop shop = cacheClient.getWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, 30L, TimeUnit.MINUTES, RedisConstants.LOCK_SHOP_KEY + id, this::getById);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

//    /**
//     * 互斥锁
//     * 1000线程 ramp-up 1秒 循环次数300， 测试吞吐量 13805 ，平均响应时间 67ms ，异常 0%
//     * @param id
//     * @return {@link Result }
//     */
//    @Override
//    public Result queryById(Long id) {
//        // 1. 先查Redis
//        Result result = queryCacheById(id);
//        // 有记录或者缓存穿透情况
//        if (result != null) {
//            return result;
//        }
//        // Redis无记录，查数据库
//        // 2. 抢锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            // 互斥锁解决缓存击穿，Redis没数据之后，查数据库之前，获取锁
//            // 使用setnx,注意，要有持锁时间，查数据库，最多10秒，并且在回写Redis之后，要释放掉锁
//            Boolean haveLock = lock(lockKey);
//            // （1）不持有锁，等待一段时间重试
//            if (!haveLock) {
//                Thread.sleep(50);
//                return queryById(id);
//            }
//
//            // （2）持有锁
//            // 双重检查锁定，线程进来之后要先查Redis，主要应对第一个线程写完之后的进来的线程
//            result = queryCacheById(id);
//            // 有记录或者缓存穿透情况
//            if (result != null) {
//                return result;
//            }
//
//            // 处理第一次拿到锁进来的线程
//            // 3. 无记录去查MySQL
//            shop = getById(id);
//
//            String key = RedisConstants.CACHE_SHOP_KEY + id;
//            // 4. 数据库也无记录，缓存低TTL的空对象，防止缓存穿透
//            if (shop == null) {
//                stringRedisTemplate.opsForValue().set(key, "NULL", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                // 数据库无记录，也需要释放掉缓存击穿的锁，否则第一次之后的线程会白白被锁住直到锁过期
//                return Result.fail("店铺不存在！");
//            }
//            // 5. 有记录回写Redis并返回
//            stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//            return Result.ok(shop);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 6. 当前线程处理完之后，必须释放锁
//            unlock(lockKey);
//        }
//    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺ID不能为null");
        }
        // 1. 更新数据库
        updateById(shop);
        // 2. 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShop(Shop shop) {
        // 1. 写入数据库
        save(shop);
        // 2. 同步删除原缓存(数据缓存的更新策略)
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
    }
}

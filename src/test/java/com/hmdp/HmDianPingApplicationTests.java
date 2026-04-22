package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);

    @Autowired
    private IShopService shopService;

    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将店铺经纬度信息加载到Redis
     *
     */
    @Test
    void loadShopInfo(){
        // 1. 查询店铺信息
        List<Shop> list = shopService.query().list();
        if (list == null || list.isEmpty()) {
            return;
        }
        // 2. 导入 键值对信息： shop:geo:typeId -> [shopId, x坐标, y坐标]
        for (Shop shop : list) {
            stringRedisTemplate.opsForGeo().add(RedisConstants.SHOP_GEO_KEY + shop.getTypeId(),
                    new RedisGeoCommands.GeoLocation<>(shop.getId().toString(), new Point(shop.getX(), shop.getY())));
        }
    }




//    @Test
//    void testRedissonClient() throws InterruptedException {
//        // 创建锁（可重入）
//        RLock lock = redissonClient.getLock("anyLock");
//        // tryLock 参数：获取锁最大等待时间，锁自动释放时间，单位
//        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
//        // 获取锁成功
//        if (isLock) {
//            try{
//                System.out.println("执行业务！");
//            }finally {
//                // 释放锁
//                lock.unlock();
//            }
//        }
//    }



//    // 预热
////    @Test
//    void contextLoads() {
//        Shop shop = shopService.getById(1L);
//        cacheClient.setWithLogicalExpire(shop, RedisConstants.CACHE_SHOP_KEY + 1L, RedisConstants.CACHE_SHOP_TTL, TimeUnit.SECONDS);
//    }
//
////    @Test
//    void testIdWorker() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(300);
//
//        Runnable task = () -> {
//            for (int i = 0; i < 100; i++) {
//                long id = redisIdWorker.generateId("order");
//                System.out.println("id = " + id);
//            }
//            latch.countDown();
//        };
//        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 300; i++) {
//            EXECUTOR_SERVICE.submit(task);
//        }
//        latch.await();
//        long end = System.currentTimeMillis();
//        System.out.println("time = " + (end - begin));
//    }




}

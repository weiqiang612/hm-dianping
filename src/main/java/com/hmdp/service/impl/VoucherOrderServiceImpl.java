package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {


    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    // 这里使用静态变量，DefaultRedisScript 内部会在第一次执行后缓存脚本的 SHA1，
    // 后续调用优先走 EVALSHA，脚本内容不用反复传输，高并发下效果显著
    // 泛型为Lua脚本返回值类型
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    private IVoucherOrderService proxy;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setResultType(Long.class);
        SECKILL_SCRIPT.setLocation(new ClassPathResource("scripts/seckill.lua"));
    }

    public static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 当前类初始化后执行
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    // 异步线程一直在等待阻塞队列中是否有订单信息，如果有就取出订单信息进行处理
    private class VoucherOrderHandler implements Runnable {

        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                // 1. 查看消息队列是否有订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders > // 读取消息，并添加到 pending list
                // 返回值形式 stream 名称（如 stream.orders）+ 消息 id + 消息体 Map<field, value>
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(queueName, ReadOffset.lastConsumed())
                );
                // 2. 判断消息是否获取成功
                if (list == null || list.isEmpty()) {
                    // 2.2 没有获取到消息就下一轮循环
                    continue;
                }
                // 2.1 成功就创建订单，需要ACK
                try {
                    // 3. 正常处理订单
                    // 3.1 解析消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    // 3.2 根据消息体创建订单
                    // 获取 voucherId 、 userId 、 orderId
                    // redis.call('XADD', 'stream.orders', '*', 'voucherId', voucherId, 'userId', userId, 'id', orderId)
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // 3.3 ack 消息
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    // 4. 如果有异常，就循环处理pending队列中的未确认消息
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
            /*while (true) {
                // 1. 查看阻塞队列是否有订单信息
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2. 有就创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }*/
        }

        private void handlePendingList() {
            while (true) {
                // 1. 获取pending队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream.orders 0 // 不传 < 代表从 pending list读取
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                // 2. pending list 无消息，说明处理完毕
                if (list == null || list.isEmpty()) {
                    break;
                }
                try {
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    // 3.2 根据消息体创建订单
                    // 获取 voucherId 、 userId 、 orderId
                    // redis.call('XADD', 'stream.orders', '*', 'voucherId', voucherId, 'userId', userId, 'id', orderId)
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // 3.3 ack 消息
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pending list订单异常", e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }

    }

    // 扣减库存，创建订单，数据库中的操作
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 获取锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        boolean success = lock.tryLock();
        // 获取锁失败返回错误信息，因为锁的粒度在一个用户的级别，只有相同用户的操作才会竞争锁
        if (!success) {
            log.error("不允许重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }


    /**
     * 优惠券秒杀下单(异步版本)
     *
     * @param voucherId
     * @return {@link Result }
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
/*        // 1. 查询优惠券信息
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("订单不存在！");
        }
        // 2. 是否在时间段内，不在时间段内需要返回错误信息
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (LocalDateTime.now().isBefore(beginTime) || LocalDateTime.now().isAfter(endTime)) {
            return Result.fail("请在规定时间内抢购！");
        }
        // 3. 判断库存是否充足
        // 4. 库存不足返回错误信息
        // 5. 一人一单
        // 有对keys为null的处理
//        final int keySize = keys != null ? keys.size() : 0;
        long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                UserHolder.getUser().getId().toString());
        if (res != 0) {
            return Result.fail(res == 1 ? "库存不足！" : "一个人只允许下一单！");
        }
        // 6. 扣减库存、下单
        // 7. 将优惠券ID、用户ID和订单ID存入阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        Long orderId = redisIdWorker.generateId("order");
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        // 这里保存到阻塞队列，由单独线程去消费，异步执行下单逻辑
        orderTasks.add(voucherOrder);
        // 主线程将proxy对象拿出来，子线程直接使用
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 8. 返回订单ID
        return Result.ok(orderId);*/
        // 1. 查询优惠券信息
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("订单不存在！");
        }
        // 2. 是否在时间段内，不在时间段内需要返回错误信息
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (LocalDateTime.now().isBefore(beginTime) || LocalDateTime.now().isAfter(endTime)) {
            return Result.fail("请在规定时间内抢购！");
        }
        // 3. 判断库存是否充足
        // 4. 库存不足返回错误信息
        // 5. 一人一单
        // 有对keys为null的处理
//        final int keySize = keys != null ? keys.size() : 0;
        // 脚本需要再传递 orderId参数
        Long orderId = redisIdWorker.generateId("order");
        long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                UserHolder.getUser().getId().toString(),
                orderId.toString());
        if (res != 0) {
            return Result.fail(res == 1 ? "库存不足！" : "一个人只允许下一单！");
        }
        // 6. 扣减库存、下单
        // 主线程将proxy对象拿出来，子线程直接使用
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 8. 返回订单ID
        return Result.ok(orderId);
    }

//    /**
//     * 优惠券秒杀下单(同步版本)
//     *
//     * @param voucherId
//     * @return {@link Result }
//     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1. 查询优惠券信息
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        if (seckillVoucher == null) {
//            return Result.fail("订单不存在！");
//        }
//        // 2. 是否在时间段内，不在时间段内需要返回错误信息
//        LocalDateTime beginTime = seckillVoucher.getBeginTime();
//        LocalDateTime endTime = seckillVoucher.getEndTime();
//        if (LocalDateTime.now().isBefore(beginTime) || LocalDateTime.now().isAfter(endTime)) {
//            return Result.fail("请在规定时间内抢购！");
//        }
//        // 3. 判断库存是否充足
//        // 4. 库存不足返回错误信息
//        if (seckillVoucher.getStock() < 1) {
//            return Result.fail("库存不足！");
//        }
//        Long userId = UserHolder.getUser().getId();
//        // 获取锁
//        RLock lock = redissonClient.getLock("lock:order:" + userId);

    //        // SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        boolean success = lock.tryLock();
//        // 获取锁失败返回错误信息，因为锁的粒度在一个用户的级别，只有相同用户的操作才会竞争锁
//        if (!success) {
//            return Result.fail("一个人只允许下一单！");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5. 一人一单
        // 5.1 根据用户ID和优惠券ID查询数据库，是否存在
        Long userId = voucherOrder.getUserId();
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            // 5.2 存在返回错误信息
            log.error("您已经抢购过该优惠券！");
            return;
        }

        // 6. 库存充足开始下单逻辑
        // 6.1 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .ge("stock", 1)
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足！");
            return;
        }

        // 7. 创建订单
        save(voucherOrder);
    }
}

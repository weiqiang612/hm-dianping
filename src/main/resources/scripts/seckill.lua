-- 1. 参数列表
-- 1.1 优惠券ID
local voucherId = ARGV[1]
-- 1.2 用户ID
local userId = ARGV[2]
-- 1.3 订单ID
local orderId = ARGV[3]

-- 2. 数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order' .. voucherId

local stock = redis.call('get', stockKey)
-- 3. 判断库存是否充足
if tonumber(stock) <= 0 then
    return 1
end
-- 4. 判断用户是否已经下单
local ordered = redis.call('SISMEMBER', orderKey,userId)
if ordered == 1 then
    return 2
end
-- 5. 扣减库存
redis.call('DECR', stockKey)
-- 6. 将userID存入set集合
redis.call('SADD', orderKey,userId)
-- 7. 向stream.orders中添加消息
redis.call('XADD', 'stream.orders', '*', 'voucherId', voucherId, 'userId', userId, 'id', orderId)


return 0
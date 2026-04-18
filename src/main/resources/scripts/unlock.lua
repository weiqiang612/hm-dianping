-- 如果取出来的值和传过来的值(线程标识)相等（判断是否为自己的锁），释放锁
if(redis.call('get',KEYS[1]) == ARGV[1]) then
    -- 释放锁
    return redis.call('del',KEYS[1])
end
return 0
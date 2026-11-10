-- 秒杀原子扣库存（含一人一单）——教程 stage4/01
-- KEYS[1] = 库存 key    seckill:stock:1
-- KEYS[2] = 已购集合     seckill:users:1
-- ARGV[1] = userId
-- 判断+扣减+登记必须在 Lua 里原子执行（等价于服务端互斥锁），任何"GET 再判断再 DECR"的
-- 应用层写法在并发下都会超卖。
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2                                   -- 该用户已抢过（一人一单）
end
local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then
    return 0                                    -- 售罄
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1                                        -- 预扣成功

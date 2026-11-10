-- 秒杀 Redis 原子预扣（教程 stage4/01）
-- KEYS[1] = 库存 key      seckill:stock:{skuId}
-- KEYS[2] = 已购用户集合   seckill:users:{skuId}
-- ARGV[1] = userId
-- 判断 + 扣减 + 记录必须原子，否则并发下超卖
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2                                   -- 该用户已抢过（一人一单）
end
local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then
    return -1                                   -- 售罄
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1                                        -- 预扣成功，发 MQ 异步建单

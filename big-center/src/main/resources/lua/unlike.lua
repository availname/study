-- 取消点赞 Lua：判存在(SREM) + 计数(DECR，防负数) 原子执行
-- KEYS[1] = 点赞集合 bc:like:set:{targetId}
-- KEYS[2] = 计数器   bc:like:cnt:{targetId}
-- ARGV[1] = userId
if redis.call('SREM', KEYS[1], ARGV[1]) == 0 then
    return -1                                   -- 本来就没点过
end
if tonumber(redis.call('GET', KEYS[2]) or '0') > 0 then
    redis.call('DECR', KEYS[2])
end
return 1

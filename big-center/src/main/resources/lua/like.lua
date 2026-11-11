-- 点赞 Lua：判重(SADD 返回值) + 计数(INCR) 原子执行——三条命令不再分步（java-core/14 题一）
-- KEYS[1] = 点赞集合 bc:like:set:{targetId}
-- KEYS[2] = 计数器   bc:like:cnt:{targetId}
-- ARGV[1] = userId
if redis.call('SADD', KEYS[1], ARGV[1]) == 0 then
    return -1                                   -- 已点过，幂等拒绝
end
redis.call('INCR', KEYS[2])
return 1                                        -- 点赞成功

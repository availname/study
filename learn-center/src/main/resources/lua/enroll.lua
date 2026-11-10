-- 限量抢课 Lua：判断(已购/售罄) + 扣名额 + 登记，原子执行
-- KEYS[1] = 剩余名额 lc:enroll:quota:{courseId}
-- KEYS[2] = 已购用户集合 lc:enroll:users:{courseId}
-- ARGV[1] = userId
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2                                   -- 已抢过（一人一课）
end
local quota = tonumber(redis.call('GET', KEYS[1]) or '-1')
if quota == nil or quota <= 0 then
    return 0                                    -- 名额已满
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1                                        -- 预扣成功

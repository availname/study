-- 滑动窗口限流：ZSET 存请求时间戳，Lua 原子执行（java-core/12 题三 + r-stage3/01）
-- KEYS[1] = 限流 key（bc:rate:{接口}:{维度值}）
-- ARGV[1] = 当前毫秒时间戳
-- ARGV[2] = 窗口毫秒（如 60000）
-- ARGV[3] = 窗口内最大请求数
-- 返回：1=放行，0=超限拒绝
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])   -- 清窗口外的旧记录
local n = redis.call('ZCARD', KEYS[1])
if n >= tonumber(ARGV[3]) then
    return 0                                                    -- 超限拒绝（快速失败）
end
redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. '-' .. math.random())  -- 记录本次（member 唯一防覆盖）
redis.call('PEXPIRE', KEYS[1], ARGV[2])                         -- key 生命周期=窗口，自动清理冷数据
return 1

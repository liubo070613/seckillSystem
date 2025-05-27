-- 获取活动状态
local activityKey = string.format("seckill:activity:%s", KEYS[1])
local activity = redis.call('get', activityKey)
if not activity then
    return -2  -- 活动不存在
end

-- 获取库存
local stockKey = string.format("seckill:stock:%s", KEYS[1])
local stock = redis.call('get', stockKey)
if not stock then
    return -2  -- 活动不存在
end

-- 转换为数字
stock = tonumber(stock)
local rollbackStock = tonumber(ARGV[1])
-- 增加库存
redis.call('incrby', stockKey, rollbackStock)

-- 删除用户秒杀资格
local userKey = string.format("seckill:user:%s:%s", KEYS[1], ARGV[2])
redis.call('del', userKey)

-- 返回新的库存数量
return stock + rollbackStock 
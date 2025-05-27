-- 获取商品库存
local stock = redis.call('get', KEYS[1])
-- 如果key不存在，返回-2表示活动不存在
if not stock then
    return -2
end

-- 检查用户是否已获得秒杀资格
local userKey = string.format("seckill:user:%s:%s", KEYS[1], ARGV[1])
if redis.call('exists', userKey) == 1 then
    return -3  -- 返回-3表示重复秒杀
end

-- 转换为数字
stock = tonumber(stock)
-- 如果库存小于等于0，返回-1表示库存不足
if stock <= 0 then
    return -1
end

-- 扣减库存
redis.call('decr', KEYS[1])
-- 记录用户秒杀资格
redis.call('set', userKey, 1, 'EX', 86400)  -- 设置24小时过期
-- 返回剩余库存
return stock - 1 
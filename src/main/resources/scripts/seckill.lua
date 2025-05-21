-- 获取商品库存
local stock = tonumber(redis.call('get', KEYS[1]))
-- 如果库存小于等于0，返回-1表示库存不足
if stock <= 0 then
    return -1
end
-- 扣减库存
redis.call('decr', KEYS[1])
-- 返回剩余库存
return stock - 1 
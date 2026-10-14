--1.参数列表
--1.1 优惠券id
local voucherId=ARGV[1]
--1.2 用户id
local userId=ARGV[2]

--订单id
local orderId=ARGV[3]

--2.数据key
--库存key
local stockKey='seckill:stock:'..voucherId
--订单key
local orderKey='seckill:order:'..voucherId
--脚本业务

--判断库存是否充足
if(tonumber(redis.call('get',stockKey))<=0) then
    return 1
end

--判断用户是否下过单
if(redis.call('sismember',orderKey,userId)==1) then
    return 2
end
--扣减库存
redis.call('incrby',stockKey,-1)
--下单
redis.call('sadd',orderKey,userId)

--发送消息到队列当中 XADD stream.orders * k1 v1 k2 v2
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)

return 0
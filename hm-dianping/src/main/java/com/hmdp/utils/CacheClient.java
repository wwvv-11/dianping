package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;


    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);

    }


    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }


    //解决缓存穿透
    public  <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R>type, Function<ID,R> dbFallback,Long time,TimeUnit unit){
        String key=keyPrefix+id;
        //从redis查商铺缓存
        String json=stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if(StrUtil.isNotBlank(json)){
           return JSONUtil.toBean(json,type);
        }

        //判断命中的是否是空值
        if(json!=null){
            //返回错误信息
            return null;
        }

        //不存在 根据id查询数据库
        R r=dbFallback.apply(id);

        //不存在数据库 返回错误
        if(r==null){

            //将空值写入redis（key是当前查询的店铺ID，value为空字符串，防止缓存穿透）
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在 写回redis
       this.set(key,r,time,unit);

        //返回
        return r;
    }


    public  <R,ID> R queryWithLogicalExpire(String keyPrefix,ID id,Class<R>type,Function<ID,R>dbFallback,Long time,TimeUnit unit){
        //keyPrefix  cache:xxx
       String key=keyPrefix+id;
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        //为空 返回null
        if(jsonStr==null||jsonStr.isEmpty()){
            return null;
        }
        //不为空
        RedisData redisData = JSONUtil.toBean(jsonStr, RedisData.class);
        LocalDateTime expireTime=redisData.getExpireTime();
        JSONObject data = (JSONObject) redisData.getData();
        R r=JSONUtil.toBean(data,type);

        if(expireTime.isAfter(LocalDateTime.now())){
            //过期时间在现在时间之后 未过期
            //直接返回
            return r;
        }

        //过期
        //拿锁
        String lockKey=keyPrefix.replace("cache:","lock:")+id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
                CACHE_REBUILD_EXECUTOR.submit(()->{
                    try {
                        R r1= dbFallback.apply(id);
                        this.setWithLogicalExpire(key,r1,time,unit);
                    }finally {
                        unlock(lockKey);
                    }
                });
        }
        return r;
    }
    private boolean tryLock(String key){
        Boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  void unlock(String key){
        stringRedisTemplate.delete(key);
    }


}

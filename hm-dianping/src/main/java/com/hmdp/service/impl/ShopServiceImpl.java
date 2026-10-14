package com.hmdp.service.impl;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @PostConstruct
    public void init(){
        preheatShopName2Id();
    }


    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop=querWithPassThrough(id);
        //Shop shop=cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop=querWithMutex(id);

        //用逻辑过期解决缓存击穿
        Shop shop=cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);


        if(shop==null){
            return Result.fail("店铺不存在");
        }
        //返回
        return Result.ok(shop);
    }

//    private Shop querWithMutex(Long id) {
//        String key=CACHE_SHOP_KEY+id;
//        //从redis查商铺缓存
//        String shopJson=stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //存在 直接返回
//            Shop shop= JSONUtil.toBean(shopJson,Shop.class);
//            return shop;
//        }
//
//        //判断命中的是否是空值
//        if(shopJson!=null){
//            //返回错误信息
//            return null;
//        }
//
//
//        //实现缓存重建
//        //获取互斥锁
//        String lockKey="lock:shop:"+id;
//        Shop shop= null;
//        try {
//            boolean isLock=tryLock(lockKey);
//
//            //判断是否获取成功
//            if(!isLock){
//                //失败 则休眠并重试
//                Thread.sleep(50);
//                return querWithMutex(id);
//            }
//
//
//
//            //成功 根据id查询数据库
//            shop = getById(id);
//
//            //不存在数据库 返回错误
//            if(shop==null){
//                //将空值写入redis（key是当前查询的店铺ID，value为空字符串，防止缓存穿透）
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//            //存在 写回redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL+ RandomUtil.randomLong(10), TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放互斥锁
//            unlock(lockKey);
//        }
//
//        //返回
//        return shop;
//    }


    //尝试获取锁
//    private boolean tryLock(String key){
//        Boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    private  void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

//    public  Shop queryWithLogicalExpire(Long id){
//        String key=CACHE_SHOP_KEY+id;
//        //从redis查商铺缓存
//        String shopJson=stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if(StrUtil.isBlank(shopJson)){
//            return null;
//        }
//
//        //命中 需要判断过期时间 需要先把json反序列化为对象
//        RedisData redisData=JSONUtil.toBean(shopJson,RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime=redisData.getExpireTime();
//        //判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            //未过期 直接返回店铺信息
//            return shop;
//        }
//        //已过期 需要缓存重建
//
//       String lockKey=LOCK_SHOP_KEY+id;
//        boolean isLock = tryLock(lockKey);
//
//        if(isLock){
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    this.saveShop2Redis(id,30L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    unlock(lockKey);
//                }
//            });
//
//        }
//
//        //返回
//        return shop;
//    }

//    public  Shop queryWithPassThrough(Long id){
//        String key=CACHE_SHOP_KEY+id;
//        //从redis查商铺缓存
//        String shopJson=stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //存在 直接返回
//            Shop shop= JSONUtil.toBean(shopJson,Shop.class);
//            return shop;
//        }
//
//        //判断命中的是否是空值
//        if(shopJson!=null){
//            //返回错误信息
//            return null;
//        }
//
//        //不存在 根据id查询数据库
//        Shop shop=getById(id);
//
//        //不存在数据库 返回错误
//        if(shop==null){
//
//            //将空值写入redis（key是当前查询的店铺ID，value为空字符串，防止缓存穿透）
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //存在 写回redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL+ RandomUtil.randomLong(10), TimeUnit.MINUTES);
//
//        //返回
//        return shop;
//    }
    public void saveShop2Redis(Long id,Long expireSeconds)  {
        //查询店铺数据
        Shop shop=getById(id);

        //封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Transactional
    @Override
    public Result update(Shop shop)  {
        Long id=shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);

        //更改缓存
        saveShop2Redis(shop.getId(), CACHE_SHOP_TTL);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //判断是否需要根据坐标查询
        if(x==null||y==null){
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //计算分页参数
        int from= (current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;


        //查询redis
        String key=SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));

        //解析出id
        if(results==null){
          return  Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size()<=from){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids=new ArrayList<>(list.size());
        Map<String,Distance>distanceMap=new HashMap<>(list.size());
        //截取from到end的部分
        list.stream().skip(from).forEach(result->{
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            Distance distance=result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });

        //根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for(Shop shop:shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    @Override
    public void saveShop2DBAndRedis(Shop shop) {
        save(shop);
        saveShop2Redis(shop.getId(),CACHE_SHOP_TTL);
    }

    public Result queryIdByName(String name){
        Object id = stringRedisTemplate.opsForHash().get(SHOP_NAME2ID_KEY, name);
        if(id!=null){
            return Result.ok(Long.valueOf(id.toString()));
        }

        //hash没命中
        Shop shop=this.query().like("name",name).last("limit 1").one();
        if(shop==null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop.getId());
    }


    //启动时全量预热
    public void preheatShopName2Id(){
        List<Shop>shops=list();
        Map<String,String> map=new HashMap<>();
        for (Shop shop:shops){
            map.put(shop.getName(), String.valueOf(shop.getId()));
        }
        stringRedisTemplate.opsForHash().putAll(SHOP_NAME2ID_KEY,map);
    }
}

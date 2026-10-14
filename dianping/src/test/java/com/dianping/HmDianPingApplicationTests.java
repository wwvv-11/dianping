package com.dianping;

import com.dianping.entity.Shop;
import com.dianping.service.impl.ShopServiceImpl;
import com.dianping.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.CACHE_SHOP_TTL;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

   @Resource
    private RedisIdWorker redisIdWorker;

   private ExecutorService es= Executors.newFixedThreadPool(500);


   @Resource
   private StringRedisTemplate stringRedisTemplate;

   @Test
    void testIdWorker() throws InterruptedException {
       CountDownLatch latch=new CountDownLatch(300);
       Runnable task=()->{
           for (int i = 0; i < 100; i++) {
               long id = redisIdWorker.nextId("order");
               System.out.println("id="+id);
           }
           latch.countDown();
       };
       long begin=System.currentTimeMillis();
       for (int i = 0; i < 300; i++) {
           es.submit(task);
       }
       latch.await();
       long end=System.currentTimeMillis();
       System.out.println("time="+(end-begin));
   }

   @Test
    void loadShopData(){
       //查询店铺信息
       List<Shop> list = shopService.list();
       //将店铺分组  按照typeId分组  id一致的放到一个集合
       Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(shop->shop.getTypeId()));


       //分批完成写入redis
       for(Map.Entry<Long,List<Shop>>entry:map.entrySet()){
           Long typeId= entry.getKey();
           String key="shop:geo:"+typeId;
           List<Shop>value=entry.getValue();

           List<RedisGeoCommands.GeoLocation<String>>locations=new ArrayList<>(value.size());
           //写入redis geo add key  经度 纬度
           for(Shop shop:value){
               locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(), shop.getY())));
               //stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
           }
           stringRedisTemplate.opsForGeo().add(key,locations);

       }
   }



   @Test
    void loadShopCache(){
       List<Shop>list=shopService.list();
       for (Shop shop:list){
           shopService.saveShop2Redis(shop.getId(),CACHE_SHOP_TTL);
       }
   }

}

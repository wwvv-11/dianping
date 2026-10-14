package com.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.dianping.dto.Result;

import com.dianping.entity.ShopType;
import com.dianping.mapper.ShopTypeMapper;
import com.dianping.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.dianping.utils.RedisConstants.CACHE_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public Result queryTypeList() {
        String key=CACHE_TYPE_KEY;
        String typeJson=stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(typeJson)){
            List<ShopType>typeList=JSONUtil.toList(typeJson,ShopType.class);
            return Result.ok(typeList);
        }

        List<ShopType> typeList=query().orderByAsc("sort").list();
        if(typeList==null){
            return Result.fail("发生错误");
        }

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(typeList));

        return Result.ok(typeList);


    }
}

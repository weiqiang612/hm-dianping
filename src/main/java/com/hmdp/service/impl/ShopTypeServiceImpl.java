package com.hmdp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     *
     * 查询商店类型
     *
     * @return {@link List }<{@link ShopType }>
     */
    @Override
    public List<ShopType> queryTypeList() {
        // 1. 查Redis
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        String shopTypeJSON = stringRedisTemplate.opsForValue().get(key);
        // 2. 有结果直接返回
        if (StrUtil.isNotBlank(shopTypeJSON)) {
            List<ShopType> list = JSONUtil.toList(shopTypeJSON, ShopType.class);
            // 续期
            stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
            return list;
        }
        // 3. 无结果查询MySQL
        List<ShopType> list = query().orderByAsc("sort").list();
        // 4. MySQL有结果回写Redis并返回
        if (CollUtil.isNotEmpty(list)) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(list), RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        }
        return list;
    }
}

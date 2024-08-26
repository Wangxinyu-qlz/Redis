package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
	@Override
	public Result queryShopById(Long id) {
		String key = CACHE_SHOP_KEY + id;
		//query shop cache from redis
		String shopJson = stringRedisTemplate.opsForValue().get(key);
		//if exist
		//isNotBlank: null  ""  空格、全角空格、制表符、换行符，等不可见字符
		if (StrUtil.isNotBlank(shopJson)) {
			//return
			Shop shop = JSONUtil.toBean(shopJson, Shop.class);
			return Result.ok(shop);
		}
		//追加判断是否为""
		if (shopJson != null && shopJson.equals("")) {
			return Result.fail("店铺不存在");
		}
		//if not exist, query DB by ID
		Shop shop = getById(id);
		//if not exist, return error
		if (shop == null) {
			//解决缓存穿透：缓存空数据，设置较短的过期时间
			stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
			return Result.fail("店铺不存在！");
		}
		//set expireTime
		stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
		return Result.ok(shop);
	}

	@Override
	@Transactional
	//TODO 无法保证本地数据库和redis之间的事务同步
	public Result update(Shop shop) {
		Long id = shop.getId();
		if (id == null) {
			return Result.fail("商铺id为空");
		}
		//update DB
		updateById(shop);
	    //del redis
		stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
		return Result.ok();
	}
}

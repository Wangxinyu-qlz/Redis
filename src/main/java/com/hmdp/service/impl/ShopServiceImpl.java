package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
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

	//获取互斥锁
	private boolean tryLock(String key) {
		Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
		//TODO 警惕拆箱空指针
		return BooleanUtil.isTrue(aBoolean);
	}

	//释放互斥锁
	private void unlock(String key) {
		stringRedisTemplate.delete(key);
	}

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

		//TODO 实现缓存重建
		//获取互斥锁
		String lock = LOCK_SHOP_KEY + id;
		Shop shop = null;
		//TODO 抛异常的情况下，也需要释放锁，在finally中执行释放锁的逻辑
		try {
			boolean triedLock = tryLock(lock);
			//判断是否获取成功
			//失败，休眠并重试
			if (!triedLock) {
				Thread.sleep(200);
				//重试
				return queryShopById(id);
			}
			//成功，根据id查询数据库
			//if not exist, query DB by ID
			shop = getById(id);
			//if not exist, return error
			if (shop == null) {
				//解决缓存穿透：缓存空数据，设置较短的过期时间
				stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
				return Result.fail("店铺不存在！");
			}
			//set expireTime
			stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			//释放互斥锁
			unlock(lock);
		}
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

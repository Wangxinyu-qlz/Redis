package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.transaction.TransactionUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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

	//get mutual exclusion lock
	private boolean tryLock(String key) {
		Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
		//TODO 警惕拆箱空指针
		return BooleanUtil.isTrue(aBoolean);
	}

	//release mutual exclusion lock
	private void unlock(String key) {
		stringRedisTemplate.delete(key);
	}

	//logical expire 缓存击穿
	@Override
	public Result queryShopById(Long id) {
		ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
		String key = CACHE_SHOP_KEY + id;
		String shopJson = stringRedisTemplate.opsForValue().get(key);
		if(StrUtil.isBlank(shopJson)) {
			return Result.fail("店铺信息不存在");
		}
		RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
		//还需要再转换一次
		Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
		LocalDateTime expireTime = redisData.getExpireTime();
		//not expired
		if(expireTime.isAfter(LocalDateTime.now())) {
			return Result.ok(shop);
		}
		//expired, get lock and rebuild cache
		String lockKey = LOCK_SHOP_KEY + id;
		boolean triedLock = tryLock(lockKey);
		//get lock
		if(triedLock) {
			CACHE_REBUILD_EXECUTOR.submit(() -> {
				try {
					//rebuild cache
					this.saveShop2Redis(id, 20L);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					unlock(lockKey);
				}
			});
		}
		return Result.ok(shop);
	}

	@Override
	@Transactional(timeout = 6)
	//TODO 无法保证本地数据库和redis之间的事务同步
	public Result update(Shop shop) {
		Long id = shop.getId();
		if (id == null) {
			return Result.fail("商铺id为空");
		}

		// 模拟抛出异常以触发事务回滚
		try {
			System.out.println("Starting sleep...");
			Thread.sleep(5000); // 模拟长时间操作
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted Exception", e);
		}
		//写入数据库
		TransactionUtil.doAfterTransact(() -> {
			//执行业务
			System.out.println("run sth...");
		});

		//update DB
		updateById(shop);
	    //del redis
		stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
		System.out.println("更新完成");
		//使用线程池，异步再次删除缓存
		return Result.ok();
	}

	public void saveShop2Redis(Long id, Long expireSeconds) {
		Shop shop = getById(id);
		RedisData redisData = new RedisData();
		redisData.setData(shop);
		redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
		//write to redis
		stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
	}
}

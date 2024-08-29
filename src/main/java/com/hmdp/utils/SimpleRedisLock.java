package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-28 23:01
 * @description:
 **/
public class SimpleRedisLock implements ILock {
	private String name;
	private static final String KEY_PREFIX = "lock:";
	private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
	private StringRedisTemplate stringRedisTemplate;

	public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
		this.name = name;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean tryLock(long timeoutSec) {
		String threadId = ID_PREFIX + Thread.currentThread().getId();
		Boolean success = stringRedisTemplate.opsForValue()
				.setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(success);
	}

	@Override
	public void unlock() {
		//获取线程标识
		String threadId = ID_PREFIX + Thread.currentThread().getId();
		//获取锁中的标识
		String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
		//判断是否一致
		if (threadId.equals(id)) {
			//释放锁
			stringRedisTemplate.delete(KEY_PREFIX + name);
		}
	}
}

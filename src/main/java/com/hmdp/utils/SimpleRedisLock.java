package com.hmdp.utils;

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
	private StringRedisTemplate stringRedisTemplate;

	public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
		this.name = name;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean tryLock(long timeoutSec) {
		String threadId = String.valueOf(Thread.currentThread().getId());
		Boolean success = stringRedisTemplate.opsForValue()
				.setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(success);
	}

	@Override
	public void unlock() {
		stringRedisTemplate.delete(KEY_PREFIX + name);
	}
}

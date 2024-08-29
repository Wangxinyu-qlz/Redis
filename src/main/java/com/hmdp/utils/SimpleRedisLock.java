package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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

	//TODO 初始化就加载lua脚本，不需要每次释放锁都进行IO
	private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
	static {
		UNLOCK_SCRIPT = new DefaultRedisScript<>();
		UNLOCK_SCRIPT.setLocation(new ClassPathResource("unLock.lua"));
		UNLOCK_SCRIPT.setResultType(Long.class);
	}

	public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
		this.name = name;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean tryLock(long timeoutSec) {
		String threadId = ID_PREFIX + Thread.currentThread().getId();
		//TODO setnx有以下问题：
		// 1.不可重入：当一个线程请求一个由其他线程持有的对象锁时，该线程会阻塞。
		//           当线程请求自己持有的对象锁时，如果该线程是重入锁，请求就会成功，否则阻塞。
		// 2.不可重试：失败后不能重试
		// 3.超时释放：可以避免死锁，但是业务执行时间较长，会导致锁释放
		// 4.主从一致性：如果是redis集群，主机异步同步数据给从机，完成之前主机宕机，锁失效
		Boolean success = stringRedisTemplate.opsForValue()
				.setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(success);
	}

	@Override
	public void unlock() {
		//调用lua脚本
		//拿锁 判断锁 删除锁 原子性执行
		stringRedisTemplate.execute(UNLOCK_SCRIPT,
				Collections.singletonList(KEY_PREFIX + name),
				ID_PREFIX + Thread.currentThread().getId());
	}
}

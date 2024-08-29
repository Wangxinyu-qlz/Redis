package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HmDianPingApplication.class)
class HmDianPingApplicationTests {
	@Resource
	private IShopService service;
	@Resource
	private RedisIdWorker redisIdWorker;
	@Resource
	private RedissonClient redissonClient;
	@Test
	void testSaveShop() {
		for (int i = 0; i < 15; i++) {
			service.saveShop2Redis((long)(i+1), 10L);
		}
	}

	@Test
	void testIdWorker() throws InterruptedException {
		//TODO 借助countDownLatch进行异步多线程计时
		// 让主线程 等待 异步线程全部执行完
		CountDownLatch countDownLatch = new CountDownLatch(300);

		ExecutorService es = Executors.newFixedThreadPool(500);
		Runnable task = () -> {
			for (int i = 0; i < 100; i++) {
				long order = redisIdWorker.nextId("order");
				System.out.println("id=" + order);
			}
			countDownLatch.countDown();
		};

		long start = System.currentTimeMillis();
		for (int i = 0; i < 300; i++) {
			es.submit(task);
		}
		countDownLatch.await();
		long end = System.currentTimeMillis();
		System.out.println("耗时："+ (end - start));
	}

	@Test
	void testRedisson() throws Exception{
		//获取锁（可重入），指定锁的名称
		RLock lock = redissonClient.getLock("anyLock");
		//获取锁的最大等待时间，锁自动释放时间，时间单位
		boolean triedLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
		if(triedLock) {
			try{
				System.out.println("run sth...");
			}finally {
				lock.unlock();
			}
		}
	}

	@Test
	//并不会阻止因超时而导致的事务回滚。事务超时是由事务管理器在超时后强制回滚的，而不是由测试框架的回滚设置控制的。
	//禁止回滚 spring test中，@Transactional使得事务在测试方法执行后回滚，防止对数据库中的数据造成持久的更改
	@Rollback(false)
	@Transactional()
	void testTx() throws RuntimeException {
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
	}
}

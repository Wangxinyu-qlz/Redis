package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HmDianPingApplication.class)
class HmDianPingApplicationTests {
	@Resource
	private IShopService service;
	@Resource
	private RedisIdWorker redisIdWorker;
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
}

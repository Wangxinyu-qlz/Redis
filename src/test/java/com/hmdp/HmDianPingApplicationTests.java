package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IShopService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

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

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Test
	//并不会阻止因超时而导致的事务回滚。事务超时是由事务管理器在超时后强制回滚的，而不是由测试框架的回滚设置控制的。
	//禁止回滚 spring test中，@Transactional使得事务在测试方法执行后回滚，防止对数据库中的数据造成持久的更改
	@Rollback(false)
	@Transactional(timeout = 1)
	void testTx() throws RuntimeException {
		//必须写在数据库提交之前，否则计划B不会执行
		//必须先注册事务同步，确保它在事务回滚时能被调用
		TransactionUtil.doAfterTransact(
				() -> {
					//计划A
					System.out.println("===================提交成功run sth...=======================");
				},
				//计划B
				() -> {
					System.out.println("======================发生异常，事务回滚====================");
				});
		// 模拟抛出异常以触发事务回滚
		try {
			System.out.println("Starting sleep...");
			Thread.sleep(2000); // 模拟长时间操作
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted Exception", e);
		}

		jdbcTemplate.update("update tb_user set nick_name ='小宇同学' where id = 1");

	}

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private IUserService userService;

	@Test
	public void testGetAll() {
		List<User> users = userService.list();
		users.forEach(
				user -> {
					//随机生成token,作为登录令牌
					String token = UUID.randomUUID().toString(true);
					//将User对象转化为HashMap存储
					UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
					File file = new File("src/main/resources/tokens.txt");
					FileOutputStream output = null;
					try {
						output = new FileOutputStream(file, true);
						byte[] bytes = token.getBytes();
						output.write(bytes);
						output.write("\r\n".getBytes());
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						try {
							output.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
							CopyOptions.create()
									.setIgnoreNullValue(true)
									.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
					//存储
					String tokenKey = LOGIN_USER_KEY + token;
					stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
					//设置token有效期
					stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
				}
		);
	}
}

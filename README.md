# 黑马点评：redis实战
### 1. 高并发，负载均衡，反向代理nginx
### 2. 拦截器链 刷新token有效期 -> 验证用户身份
### 3. 验证码
### 4. 缓存商铺类型 ZSet(使用sort字段进行排序) 解决穿透和击穿 雪崩
### 5. 重点难点 秒杀优惠券
#### &emsp;&emsp;5.1. 解决超卖问题：cas乐观锁：查库存 扣库存 提交前再次查看库存是否变化
#### &emsp;&emsp;5.2. 解决少买问题：cas乐观锁：查库存 扣库存 提交前再次查看库存是否剩余
#### &emsp;&emsp;5.3. 一人一单:
##### &emsp;&emsp;&emsp;&emsp; 5.3.1. synchronized锁 @Transactional 
##### &emsp;&emsp;&emsp;&emsp;5.3.2. 锁的粒度 
##### &emsp;&emsp;&emsp;&emsp;5.3.3. 事务失效 事务可见性 
##### &emsp;&emsp;&emsp;&emsp;5.3.4. String不可变
##### &emsp;&emsp;&emsp;&emsp;5.3.5. 集群下单机锁失效 
##### &emsp;&emsp;&emsp;&emsp;5.3.6. setnx锁实现误删其他线程锁/不可重入/不可重试/超时释放/主从一致性
##### &emsp;&emsp;&emsp;&emsp;5.3.7. redission分布式锁	
##### &emsp;&emsp;&emsp;&emsp;5.3.8. RabbitMQ优化异步秒杀
##### &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;5.3.8.1. 使用lua脚本保证 [判断库存+一人一单+扣减库存] 的原子性
##### &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;5.3.8.2. RabbitMQ实现异步秒杀，实现削峰
5.3.9. google guava令牌桶算法限流
### 6. 优雅地管理事务
####   &emsp;&emsp;TransactionSynchronization
####   &emsp;&emsp;TransactionSynchronizationManager
####   &emsp;&emsp;自定义TransactionUtil
####   &emsp;&emsp;回调
```java
@Test
@Rollback(false)
@Transactional(timeout = 1)
void testTx() throws RuntimeException {
    //提交：执行计划A 失败回滚：执行计划B
    //必须先注册事务同步，确保 计划A/B 在事务提交/回滚后 能被调用
    TransactionUtil.doAfterTransact(
        () -> {
          //计划A
          System.out.println("已提交，开始执行计划A");
        },
        //计划B
        () -> {
          System.out.println("已回滚，开始执行计划B");
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
```

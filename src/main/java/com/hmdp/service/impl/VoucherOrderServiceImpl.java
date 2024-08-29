package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
	@Resource
	private ISeckillVoucherService seckillVoucherService;

	@Resource
	private RedisIdWorker redisIdWorker;

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private RedissonClient redissonClient;

	private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

	static {
		SECKILL_SCRIPT = new DefaultRedisScript<>();
		SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
		SECKILL_SCRIPT.setResultType(Long.class);
	}


	@Override
	public Result seckillVoucher(Long voucherId) {
		//获取用户
		Long userId = UserHolder.getUser().getId();
		long orderId = redisIdWorker.nextId("order");
		//执行lua脚本
		Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
				Collections.emptyList(),
				voucherId.toString(),
				userId.toString(),
				String.valueOf(orderId));
		int r = result.intValue();
		if(r != 0) {
			return Result.fail(r==1?"库存不足":"不能重复下单");
		}
		//TODO 保存到RabbitMQ

		//返回订单id
		return Result.ok(orderId);
	}

	@Transactional
	public Result createVoucherOrder(Long voucherId) {
		//一人一单
		Long userId = UserHolder.getUser().getId();
		Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
		if (count.compareTo(0) > 0) {
			return Result.fail("您已经购买过了");
		}

		//TODO 扣减库存和 创建订单的顺序不能变，否则会出现库存正常订单数量超出（一倍）
		//扣减库存
		//乐观锁：查库存，扣减库存，再次比较库存是否还有，有再提交
		boolean success = seckillVoucherService.update().
				setSql("stock = stock - 1")
				.eq("voucher_id", voucherId)
				.gt("stock", 0)
				.update();
		if (!success) {
			return Result.fail("扣减失败");
		}

		//创建订单
		VoucherOrder voucherOrder = new VoucherOrder();
		long orderId = redisIdWorker.nextId("order");
		voucherOrder.setId(orderId);
		voucherOrder.setUserId(userId);
		voucherOrder.setVoucherId(voucherId);
		//写入数据库
		save(voucherOrder);

		return Result.ok(orderId);
	}
}
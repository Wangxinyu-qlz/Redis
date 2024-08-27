package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
	@Override
	@Transactional
	public Result seckillVoucher(Long voucherId) {
		//查询优惠券
		SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
		//秒杀是否开始？
		LocalDateTime beginTime = seckillVoucher.getBeginTime();
		if(beginTime.isAfter(LocalDateTime.now())) {
			return Result.fail("秒杀尚未开始");
		}
		//秒杀是否结束
		LocalDateTime endTime = seckillVoucher.getEndTime();
		if(endTime.isBefore(LocalDateTime.now())) {
			return Result.fail("秒杀已经结束了");
		}
		//库存是否充足？
		Integer stock = seckillVoucher.getStock();
		if(stock < 1) {
			return Result.fail("您来晚了");
		}

		//扣减库存
		//解决超卖
		//乐观锁：查库存，扣减库存，再次比较库存是否还有，有再提交
		boolean success = seckillVoucherService.update().
				setSql("stock = stock - 1")
				.eq("voucher_id", voucherId)
				.gt("stock", 0)
				.update();

		if(!success) {
			return Result.fail("扣减失败");
		}
		//创建订单
		VoucherOrder voucherOrder = new VoucherOrder();
		long orderId = redisIdWorker.nextId("order");
		voucherOrder.setId(orderId);
		Long userId = UserHolder.getUser().getId();
		voucherOrder.setUserId(userId);
		voucherOrder.setVoucherId(voucherId);
		//写入数据库
		save(voucherOrder);

		return Result.ok(orderId);

	}
}
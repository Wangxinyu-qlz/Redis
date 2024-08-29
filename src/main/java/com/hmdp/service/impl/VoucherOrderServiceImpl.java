package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.rabbitmq.MQSender;
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
	@Resource
	private MQSender mqSender;

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

		//发送消息到秒杀队列
		//创建订单
		VoucherOrder voucherOrder = new VoucherOrder();
		voucherOrder.setId(orderId);
		//用户id
		voucherOrder.setUserId(userId);
		//秒杀券id
		voucherOrder.setVoucherId(voucherId);
        //将信息放入MQ中
        mqSender.sendSeckillMessage(JSON.toJSONString(voucherOrder));

		//返回订单id
		return Result.ok(orderId);
	}
}
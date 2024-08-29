package com.hmdp.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.hmdp.config.RabbitMQConfig.QUEUE;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 22:44
 * @description: 消息消费者
 **/
@Service
public class MQReceiver {
	@Resource
	private IVoucherOrderService voucherOrderService;
	@Resource
	private ISeckillVoucherService seckillVoucherService;

	/**
	 * 监听秒杀队列，消费消息，下单
	 * @param msg
	 */
	@Transactional
	@RabbitListener(queues = QUEUE)
	public void receiveSeckillMessage(String msg) {
		VoucherOrder voucherOrder = JSON.parseObject(msg, VoucherOrder.class);

		//一人一单
		Long voucherId = voucherOrder.getVoucherId();
		Long userId = voucherOrder.getUserId();
		Integer count = voucherOrderService.query().eq("user_id", userId)
				.eq("voucher_id", voucherId)
				.count();
		if(count > 0) {
			return;
		}

		//TODO 扣减库存和 创建订单的顺序不能变，否则会出现库存正常订单数量超出（一倍）
		//扣减库存
		//乐观锁：查库存，扣减库存，再次比较库存是否还有，有再提交
		boolean success = seckillVoucherService.update()
				.setSql("stock = stock - 1")
				.eq("voucher_id", voucherId)
				.gt("stock", 0)//cas
				.update();
		if(!success) {
			return;
		}

		//创建订单
		voucherOrderService.save(voucherOrder);
	}
}

package com.hmdp.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.hmdp.config.RabbitMQConfig.EXCHANGE;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 22:41
 * @description: 消息发送者
 **/
@Service
public class MQSender {
	@Resource
	private RabbitTemplate rabbitTemplate;
	private static final String ROUTINGKEY = "seckill.message";

	/**
	 * 发送秒杀消息
	 */
	public void sendSeckillMessage(String msg) {
		rabbitTemplate.convertAndSend(EXCHANGE, ROUTINGKEY, msg);
	}
}

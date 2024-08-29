package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-29 22:22
 * @description:
 **/
public class RabbitMQConfig {
	public static final String QUEUE = "seckillQueue";
	public static final String EXCHANGE = "seckillExchange";
	public static final String ROUTINGKEY = "seckill.#";

	@Bean
	public Queue queue() {
		return new Queue(QUEUE);
	}

	@Bean
	public TopicExchange topicExchange() {
		return new TopicExchange(EXCHANGE);
	}

	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue()).to(topicExchange()).with(ROUTINGKEY);
	}
}

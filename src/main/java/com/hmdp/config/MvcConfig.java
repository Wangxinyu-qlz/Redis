package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-26 12:35
 * @description: 让自定义的拦截器生效
 **/
@Configuration
public class MvcConfig implements WebMvcConfigurer {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		//通用拦截器，刷新token
		registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);
		//登录拦截器
		registry.addInterceptor(new LoginInterceptor())
				.excludePathPatterns(
						"/shop/**",
						"/voucher/**",
						"/shop-type/**",
						"/upload/**",
						"/blog/hot",
						"/user/code",
						"/user/login"
				).order(1);
	}
}

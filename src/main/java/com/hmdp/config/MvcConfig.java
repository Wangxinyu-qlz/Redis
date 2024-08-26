package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-26 12:35
 * @description: 让自定义的拦截器生效
 **/
@Configuration
public class MvcConfig implements WebMvcConfigurer {
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
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

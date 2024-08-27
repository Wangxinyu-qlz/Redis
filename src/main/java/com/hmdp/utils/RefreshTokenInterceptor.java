package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-26 12:31
 * @description:
 **/
public class RefreshTokenInterceptor implements HandlerInterceptor {
	//@Resource //Error
	//TODO LoginInterceptor不是由Spring管理生命周期的
	// 不能用@Resource/@AutoWired注入
	// 只能使用构造器
	private StringRedisTemplate stringRedisTemplate;
	public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//get token from request header
		//和前端沟通这个字段的名字
		String token = request.getHeader("authorization");
		//check if token is null
		if (StrUtil.isBlank(token)) {
			return true;
		}
		//exist: get user info from redis by token
		String tokenKey = LOGIN_USER_KEY + token;
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
		if (userMap.isEmpty()) {
			return true;
		}
		// hashMap -> UserDTO
		//do not ignore errors
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
		//save to ThreadLocal
		UserHolder.saveUser(userDTO);
		//TODO refresh token's expireTime
		stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
		//let pass
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		//TODO 移除用户，避免内存泄漏
		UserHolder.removeUser();
	}
}

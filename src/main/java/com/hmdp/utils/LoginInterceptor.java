package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @program: hm-dianping
 * @author: Qiaolezi
 * @create: 2024-08-26 12:31
 * @description:
 **/
public class LoginInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//do not exist, intercept
		if (UserHolder.getUser() == null) {
			response.setStatus(401);
			return false;
		}
		//let pass
		return true;
	}

}

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
	@Override
	public Result sendCode(String phone, HttpSession session) {
		//校验手机号
		if (RegexUtils.isPhoneInvalid(phone)) {
			return Result.fail("手机号格式错误");
		}
		//符合，生成验证码
		String code = RandomUtil.randomNumbers(6);
		//保存验证码到 session
		session.setAttribute("code", code);
		//打印日志
		log.info("短信验证码发送成功，验证码: {}", code);
		//发送
		return Result.ok();
	}

	@Override
	public Result login(LoginFormDTO loginForm, HttpSession session) {
		//校验手机号
		String phone = loginForm.getPhone();
		if (RegexUtils.isPhoneInvalid(phone)) {
			return Result.fail("手机号格式错误");
		}
		//校验验证码
		String code = loginForm.getCode();
		Object cacheCode = session.getAttribute("code");
		if (cacheCode == null || !cacheCode.toString().equals(code)) {
			return Result.fail("验证码错误");
		}

		//查询用户
		User user = query().eq("phone", phone).one();
		//不存在就创建
		if (user == null) {
			user = createUserWithPhone(phone);
		}

		//session.setAttribute("user", user);//会返回密码登敏感信息
		session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
		return Result.ok();
	}

	private User createUserWithPhone(String phone) {
		User user = new User();
		user.setPhone(phone);
		user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
		save(user);
		return user;
	}
}

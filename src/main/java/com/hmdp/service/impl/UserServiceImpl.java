package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
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
	private final StringRedisTemplate stringRedisTemplate;

	public UserServiceImpl(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public Result sendCode(String phone, HttpSession session) {
		//校验手机号
		if (RegexUtils.isPhoneInvalid(phone)) {
			return Result.fail("手机号格式错误");
		}
		//符合，生成验证码
		String code = RandomUtil.randomNumbers(6);
		//save to redis  set key value ex 120
		stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, 2, TimeUnit.MINUTES);
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
		//校验验证码 from redis
		String code = loginForm.getCode();
		String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
		if (cacheCode == null || !cacheCode.toString().equals(code)) {
			return Result.fail("验证码错误");
		}

		//查询用户
		User user = query().eq("phone", phone).one();
		if (user == null) {
			//不存在就创建，并保存到数据库
			user = createUserWithPhoneAndSave(phone);
		}

		//save user info to redis
		//generate token
		String token = UUID.randomUUID().toString();
		//User -> HashMap
		UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
		//自定义转换策略，将Long -> String
		Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,
				new HashMap<>(),
				CopyOptions.create()
						.setIgnoreNullValue(true)
						.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
		);
		//save
		//generate tokenKey: "login:token:" + token
		String tokenKey = LOGIN_USER_KEY + token;
		stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
		//set expireTime
		stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
		//TODO return token
		return Result.ok(token);
	}

	private User createUserWithPhoneAndSave(String phone) {
		User user = new User();
		user.setPhone(phone);
		user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
		save(user);
		return user;
	}
}

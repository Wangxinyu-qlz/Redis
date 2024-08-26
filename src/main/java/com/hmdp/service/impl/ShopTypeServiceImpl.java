package com.hmdp.service.impl;

import cn.hutool.json.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdp.controller.ShopController;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	private ObjectMapper objectMapper = new ObjectMapper();
	@Resource
	private IShopTypeService typeService;

	@Override
	public Result queryTypeList() {
		//从redis查询所有商铺类型缓存
		Set<String> shopTypes = stringRedisTemplate.opsForZSet().range(CACHE_SHOP_TYPE_KEY, 0, -1);
		//存在，转换为 ShopType 对象列表返回
		if(!CollectionUtils.isEmpty(shopTypes)) {
			ArrayList<Object> shopTypeList = new ArrayList<>();
			for(String shopTypeJson : shopTypes) {
				try {
					ShopType shopType = objectMapper.readValue(shopTypeJson, ShopType.class);
					shopTypeList.add(shopType);
				} catch (JsonProcessingException e) {
					log.error("店铺类型数据处理错误");
					return Result.fail("数据处理错误");
				}
			}
			return Result.ok(shopTypeList);
		}
		//不存在，从数据库查询
		List<ShopType> typeList = typeService.query().orderByAsc("sort").list();
		//数据库中不存在，返回错误
		if(CollectionUtils.isEmpty(typeList)) {
			return Result.fail("商铺类型不存在");
		}
		//数据库中存在，保存到redis，设置有效期，返回
		ZSetOperations<String, String> zSet = stringRedisTemplate.opsForZSet();
		for (ShopType shopType : typeList) {
			JsonNode jsonNode = objectMapper.valueToTree(shopType);
			//将sort字段去除
			if(jsonNode.isObject()) {
				ObjectNode objectNode = (ObjectNode)jsonNode;
				objectNode.remove("sort");
			}
			//ObjectNode -> JSON
			String json;
			try {
				json = objectMapper.writeValueAsString(jsonNode);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			zSet.add(CACHE_SHOP_TYPE_KEY, json, shopType.getSort());
		}
		stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY, CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
		return Result.ok(typeList);
	}
}

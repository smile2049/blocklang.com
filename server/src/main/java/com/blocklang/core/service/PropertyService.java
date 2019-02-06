package com.blocklang.core.service;

import java.util.Optional;

public interface PropertyService {

	/**
	 * 根据属性名获取属性值
	 * 
	 * @param key 属性名，不忽略大小写
	 * @return 字符串类型的属性值
	 */
	Optional<String> findStringValue(String key);

}
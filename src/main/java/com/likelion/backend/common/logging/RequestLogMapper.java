package com.likelion.backend.common.logging;

import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for the `request` table. Implementation is generated at
 * runtime from {@code src/main/resources/mapper/RequestLogMapper.xml}.
 */
@Mapper
public interface RequestLogMapper {

	int insert(RequestLog requestLog);
}

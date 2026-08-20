package com.likelion.backend.auth.mapper;

import com.likelion.backend.auth.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    UserDto getTestUser();
}

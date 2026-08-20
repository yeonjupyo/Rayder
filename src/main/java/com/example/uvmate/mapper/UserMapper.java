package com.example.uvmate.mapper;

import com.example.uvmate.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    UserDto getTestUser();
}
package com.likelion.backend.auth.mapper;

import com.likelion.backend.auth.dto.GeneratedUserId;
import com.likelion.backend.auth.dto.UserCredentialRow;
import com.likelion.backend.auth.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserDto getTestUser();

    /** 휴대폰 번호 또는 이메일로 비밀번호 해시까지 함께 조회한다. */
    UserCredentialRow findCredentialByIdentifier(@Param("identifier") String identifier);

    boolean existsByPhone(@Param("phone") String phone);

    void insertUser(@Param("phone") String phone,
                    @Param("nickname") String nickname,
                    @Param("password") String password,
                    @Param("holder") GeneratedUserId holder);

    UserDto findById(@Param("userId") long userId);
}

package com.example.uvmate.mapper;

import com.example.uvmate.dto.GeneratedSkinmonId;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkinmonMapper {
    String findSkinTypeByResultId(@Param("resultId") int resultId);

    Integer findAppearanceId(@Param("skinType") String skinType, @Param("expressionType") String expressionType);

    void insertSkinmon(@Param("userId") int userId,
                       @Param("resultId") int resultId,
                       @Param("skinmonName") String skinmonName,
                       @Param("appearanceId") int appearanceId,
                       @Param("holder") GeneratedSkinmonId holder);
}
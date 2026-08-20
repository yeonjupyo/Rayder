package com.likelion.backend.skinmon.mapper;

import com.likelion.backend.skinmon.dto.GeneratedSkinmonId;
import com.likelion.backend.skinmon.dto.SkinmonCreateResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkinmonMapper {
    String findSkinTypeByResultId(@Param("resultId") int resultId);

    Integer findAppearanceId(@Param("skinType") String skinType, @Param("expressionType") String expressionType);

    void upsertSkinmon(@Param("userId") int userId,
                       @Param("resultId") int resultId,
                       @Param("skinmonName") String skinmonName,
                       @Param("appearanceId") int appearanceId,
                       @Param("holder") GeneratedSkinmonId holder);

    SkinmonCreateResponse findByUserId(@Param("userId") int userId);

    SkinmonCreateResponse findBySkinmonId(@Param("skinmonId") int skinmonId);

    void updateAppearance(@Param("skinmonId") int skinmonId, @Param("appearanceId") int appearanceId);
}
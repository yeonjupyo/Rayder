package com.likelion.backend.home.mapper;

import com.likelion.backend.home.dto.DailyUvStatusRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;

@Mapper
public interface HomeMapper {
    DailyUvStatusRow findTodayUvStatus(@Param("userId") int userId);
    String findSkinTypeByUserId(@Param("userId") int userId);

    void upsertTodayUvStatus(@Param("userId") int userId,
                             @Param("uvIndex") BigDecimal uvIndex,
                             @Param("exposureRate") BigDecimal exposureRate,
                             @Param("maxUvToday") BigDecimal maxUvToday);
}

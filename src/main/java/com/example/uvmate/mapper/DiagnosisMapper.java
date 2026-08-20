package com.example.uvmate.mapper;

import com.example.uvmate.dto.DiagnosisAnswerRow;
import com.example.uvmate.dto.GeneratedResultId;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DiagnosisMapper {
    void insertAnswers(@Param("list") List<DiagnosisAnswerRow> answers);

    void insertResult(@Param("userId") int userId,
                      @Param("skinType") String skinType,
                      @Param("resultSummary") String resultSummary,
                      @Param("holder") GeneratedResultId holder);
}
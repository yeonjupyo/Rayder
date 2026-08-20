package com.likelion.backend.ai.diagnosis;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiagnosisResultMapper {
	Optional<DiagnosisResult> findLatestByUserId(long userId);
}

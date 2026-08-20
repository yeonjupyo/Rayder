package com.likelion.backend.skinmon.service;

import com.likelion.backend.skinmon.dto.*;
import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.skinmon.mapper.SkinmonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkinmonService {

    private final SkinmonMapper skinmonMapper;

    /** 외형 조회 후 SKINMON 에 쓰기까지를 한 트랜잭션으로 묶는다. */
    @Transactional
    public SkinmonCreateResponse create(SkinmonCreateRequest request) {
        String skinType = skinmonMapper.findSkinTypeByResultId(request.getResultId());
        if (skinType == null) {
            throw new BusinessException("DIAGNOSIS_RESULT_NOT_FOUND",
                    "존재하지 않는 진단 결과입니다: " + request.getResultId(), HttpStatus.NOT_FOUND);
        }

        String expressionType = "happy";
        Integer appearanceId = skinmonMapper.findAppearanceId(skinType, expressionType);
        if (appearanceId == null) {
            // 참조 데이터 누락이라 클라이언트가 고칠 수 없다. 500 을 유지하되 코드로 원인을 구분한다.
            throw new BusinessException("SKINMON_APPEARANCE_NOT_FOUND",
                    "해당 피부타입/표정의 스킨몽 외형이 등록되어 있지 않습니다: " + skinType,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        GeneratedSkinmonId holder = new GeneratedSkinmonId();
        skinmonMapper.insertSkinmon(
                request.getUserId(),
                request.getResultId(),
                request.getSkinmonName(),
                appearanceId,
                holder
        );

        return new SkinmonCreateResponse(holder.getSkinmonId(), request.getSkinmonName(), skinType, expressionType);
    }
}

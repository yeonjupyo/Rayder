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

        // 재진단이어도 이름은 요청값으로 갱신, 표정은 기존 상태 유지
        SkinmonCreateResponse existing = skinmonMapper.findByUserId(request.getUserId());
        String expressionType = (existing != null) ? existing.getExpressionType() : "happy";
        String skinmonName = request.getSkinmonName();

        Integer appearanceId = skinmonMapper.findAppearanceId(skinType, expressionType);
        if (appearanceId == null) {
            // 참조 데이터 누락이라 클라이언트가 고칠 수 없다. 500 을 유지하되 코드로 원인을 구분한다.
            throw new BusinessException("SKINMON_APPEARANCE_NOT_FOUND",
                    "해당 피부타입/표정의 스킨몽 외형이 등록되어 있지 않습니다: " + skinType,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        GeneratedSkinmonId holder = new GeneratedSkinmonId();
        skinmonMapper.upsertSkinmon(
                request.getUserId(),
                request.getResultId(),
                skinmonName,
                appearanceId,
                holder
        );

        return new SkinmonCreateResponse(holder.getSkinmonId(), skinmonName, skinType, expressionType);
    }

    @Transactional(readOnly = true)
    public SkinmonCreateResponse getByUserId(int userId) {
        SkinmonCreateResponse response = skinmonMapper.findByUserId(userId);
        if (response == null) {
            throw new BusinessException("SKINMON_NOT_FOUND",
                    "등록된 스킨몽이 없습니다: userId=" + userId, HttpStatus.NOT_FOUND);
        }
        return response;
    }

    @Transactional
    public SkinmonCreateResponse updateExpression(int skinmonId, String expressionType) {
        SkinmonCreateResponse current = skinmonMapper.findBySkinmonId(skinmonId);
        if (current == null) {
            throw new BusinessException("SKINMON_NOT_FOUND",
                    "존재하지 않는 스킨몽입니다: " + skinmonId, HttpStatus.NOT_FOUND);
        }
        Integer appearanceId = skinmonMapper.findAppearanceId(current.getSkinType(), expressionType);
        if (appearanceId == null) {
            // 표정은 happy / sad 두 값만 있으므로 매칭 실패는 입력 오류로 본다.
            throw new BusinessException("INVALID_SKINMON_EXPRESSION",
                    "지원하지 않는 표정이거나 외형이 등록되어 있지 않습니다: " + expressionType,
                    HttpStatus.BAD_REQUEST);
        }
        skinmonMapper.updateAppearance(skinmonId, appearanceId);
        return new SkinmonCreateResponse(skinmonId, current.getSkinmonName(), current.getSkinType(), expressionType);
    }
}
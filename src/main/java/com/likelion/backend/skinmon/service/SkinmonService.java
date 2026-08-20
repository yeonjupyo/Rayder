package com.likelion.backend.skinmon.service;

import com.likelion.backend.skinmon.dto.*;
import com.likelion.backend.skinmon.mapper.SkinmonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkinmonService {

    private final SkinmonMapper skinmonMapper;

    public SkinmonCreateResponse create(SkinmonCreateRequest request) {
        String skinType = skinmonMapper.findSkinTypeByResultId(request.getResultId());
        if (skinType == null) {
            throw new IllegalArgumentException("존재하지 않는 진단 결과입니다: " + request.getResultId());
        }

        // 재진단이어도 이름은 요청값으로 갱신, 표정은 기존 상태 유지
        SkinmonCreateResponse existing = skinmonMapper.findByUserId(request.getUserId());
        String expressionType = (existing != null) ? existing.getExpressionType() : "happy";
        String skinmonName = request.getSkinmonName();

        Integer appearanceId = skinmonMapper.findAppearanceId(skinType, expressionType);
        if (appearanceId == null) {
            throw new IllegalStateException("해당 피부타입/표정의 스킨몽 외형이 등록되어 있지 않습니다: " + skinType);
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

    public SkinmonCreateResponse getByUserId(int userId) {
        SkinmonCreateResponse response = skinmonMapper.findByUserId(userId);
        if (response == null) {
            throw new IllegalArgumentException("등록된 스킨몽이 없습니다: userId=" + userId);
        }
        return response;
    }

    public SkinmonCreateResponse updateExpression(int skinmonId, String expressionType) {
        SkinmonCreateResponse current = skinmonMapper.findBySkinmonId(skinmonId);
        if (current == null) {
            throw new IllegalArgumentException("존재하지 않는 스킨몽입니다: " + skinmonId);
        }
        Integer appearanceId = skinmonMapper.findAppearanceId(current.getSkinType(), expressionType);
        if (appearanceId == null) {
            throw new IllegalStateException("해당 피부타입/표정의 스킨몽 외형이 등록되어 있지 않습니다: " + current.getSkinType());
        }
        skinmonMapper.updateAppearance(skinmonId, appearanceId);
        return new SkinmonCreateResponse(skinmonId, current.getSkinmonName(), current.getSkinType(), expressionType);
    }
}
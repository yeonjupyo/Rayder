package com.likelion.backend.skinmon.service;

import com.likelion.backend.skinmon.dto.*;
import com.likelion.backend.skinmon.mapper.SkinmonMapper;
import lombok.RequiredArgsConstructor;
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
            throw new IllegalArgumentException("존재하지 않는 진단 결과입니다: " + request.getResultId());
        }

        String expressionType = "happy";
        Integer appearanceId = skinmonMapper.findAppearanceId(skinType, expressionType);
        if (appearanceId == null) {
            throw new IllegalStateException("해당 피부타입/표정의 스킨몽 외형이 등록되어 있지 않습니다: " + skinType);
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

package com.example.uvmate.service;

import com.example.uvmate.constant.DiagnosisConstants;
import com.example.uvmate.dto.*;
import com.example.uvmate.mapper.DiagnosisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;

    public DiagnosisSubmitResponse submit(DiagnosisSubmitRequest request) {
        List<String> answers = request.getAnswers();
        if (answers == null || answers.size() != 7) {
            throw new IllegalArgumentException("답변은 7개여야 합니다.");
        }

        // 1. 답변 저장
        List<DiagnosisAnswerRow> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            DiagnosisAnswerRow row = new DiagnosisAnswerRow();
            row.setUserId(request.getUserId());
            row.setQuestionNo(i + 1);
            row.setQuestionContent(DiagnosisConstants.QUESTIONS.get(i));
            row.setAnswerValue(answers.get(i));
            rows.add(row);
        }
        diagnosisMapper.insertAnswers(rows);

        // 2. 점수 계산
        int[] s = answers.stream().mapToInt(DiagnosisConstants::scoreOf).toArray();
        int sensitiveScore = s[1] + s[2];    // 2,3번
        int combinationScore = s[4];          // 5번
        int dryScore = s[3] + s[5];           // 4,6번
        int oilyScore = s[0] + s[6];          // 1,7번

        String skinType;
        if (sensitiveScore >= 4) {
            skinType = "민감성";
        } else if (combinationScore >= 2) {
            skinType = "복합성";
        } else if (Math.abs(dryScore - oilyScore) <= 2) {
            skinType = "복합성";
        } else {
            skinType = dryScore > oilyScore ? "건성" : "지성";
        }

        // 3. 결과 저장
        GeneratedResultId holder = new GeneratedResultId();
        diagnosisMapper.insertResult(request.getUserId(), skinType, skinType + " 진단 결과", holder);

        return new DiagnosisSubmitResponse(holder.getResultId(), skinType + "피부");
    }
}
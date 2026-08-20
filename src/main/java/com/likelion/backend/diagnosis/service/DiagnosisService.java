package com.likelion.backend.diagnosis.service;

import com.likelion.backend.diagnosis.constant.DiagnosisConstants;
import com.likelion.backend.diagnosis.dto.*;
import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.diagnosis.mapper.DiagnosisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;

    /** 답변 7행과 결과 1행을 함께 남긴다. 둘 중 하나만 저장되면 고아 데이터가 되므로 한 트랜잭션으로 묶는다. */
    @Transactional
    public DiagnosisSubmitResponse submit(DiagnosisSubmitRequest request) {
        List<String> answers = request.getAnswers();
        if (answers == null || answers.size() != DiagnosisConstants.QUESTIONS.size()) {
            throw new BusinessException("INVALID_DIAGNOSIS_ANSWERS",
                    "답변은 " + DiagnosisConstants.QUESTIONS.size() + "개여야 합니다.", HttpStatus.BAD_REQUEST);
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
        int[] s = scoresOf(answers);
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

        // 3. 결과 저장. result_summary 에는 설명 문구를 넣어 AI 추천이 그대로 입력으로 쓸 수 있게 한다.
        DiagnosisConstants.Copy copy = DiagnosisConstants.copyOf(skinType);
        GeneratedResultId holder = new GeneratedResultId();
        diagnosisMapper.insertResult(request.getUserId(), skinType, copy.description(), holder);

        return new DiagnosisSubmitResponse(
                holder.getResultId(), skinType + "피부", copy.keywords(), copy.description());
    }

    /** 허용되지 않은 답변 문자열은 클라이언트 입력 오류이므로 400 으로 내린다. */
    private int[] scoresOf(List<String> answers) {
        int[] scores = new int[answers.size()];
        for (int i = 0; i < answers.size(); i++) {
            try {
                scores[i] = DiagnosisConstants.scoreOf(answers.get(i));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("INVALID_DIAGNOSIS_ANSWERS",
                        (i + 1) + "번 답변이 올바르지 않습니다: " + answers.get(i), HttpStatus.BAD_REQUEST);
            }
        }
        return scores;
    }
}

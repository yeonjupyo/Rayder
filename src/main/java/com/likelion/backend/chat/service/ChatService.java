package com.likelion.backend.chat.service;

import com.likelion.backend.chat.dto.ChatRequest;
import com.likelion.backend.chat.dto.ChatResponse;
import com.likelion.backend.chat.dto.GeneratedConversationId;
import com.likelion.backend.chat.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;

    public ChatResponse chat(ChatRequest request) {
        int conversationId = getOrCreateConversation(request.getUserId());

        chatMapper.insertMessage(conversationId, "USER", request.getMessage());

        String reply = generateReply(request.getMessage());
        chatMapper.insertMessage(conversationId, "BOT", reply);

        return new ChatResponse(reply);
    }

    private int getOrCreateConversation(int userId) {
        Integer existing = chatMapper.findConversationId(userId);
        if (existing != null) {
            return existing;
        }
        GeneratedConversationId holder = new GeneratedConversationId();
        chatMapper.insertConversation(userId, holder);
        return holder.getConversationId();
    }

    private String generateReply(String message) {
        if (message.matches(".*(선크림|자외선|uv|spf|SPF|UV).*")) {
            return "자외선 지수가 높은 날에는 SPF50+ / PA++++ 제품을 권해요.\n"
                    + "'+' 표시는 UVA 차단 정도를 뜻하고, 개수가 많을수록 차단력이 높아요.\n"
                    + "2~3시간마다 덧바르면 훨씬 효과적이에요.";
        }
        if (message.matches(".*(세안|클렌징|씻).*")) {
            return "미온수로 30초 안에 마무리하는 것이 좋아요.\n"
                    + "약산성 클렌저를 쓰고, 세안 후 3분 안에 토너와 크림으로 수분을 잡아주세요.";
        }
        if (message.matches(".*(순서|루틴).*")) {
            return "아침에는 클렌저 → 토너 → 세럼 → 크림 → 선크림 순서를 지켜주세요.\n"
                    + "저녁에는 클렌징 오일로 선크림을 먼저 정리하고 같은 순서로 마무리하면 좋아요.";
        }
        if (message.matches(".*(추천|제품|뭐 바|사야).*")) {
            return "오늘은 수분 보충이 우선이에요.\n"
                    + "세라마이드 세럼과 유분감 있는 수분크림, 그리고 알로에 성분의 진정 팩을 함께 쓰면 좋아요.";
        }
        if (message.matches(".*(따갑|열|붉|빨개|가렵).*")) {
            return "자극이 올라온 상태라면 진정이 먼저예요.\n"
                    + "찬 수건으로 열을 내리고, 향료와 알코올이 없는 제품만 쓰면서 하루 정도 세럼을 쉬어가세요.";
        }
        return "오늘은 자외선 지수가 높고 미세먼지가 나쁜 날이에요.\n"
                + "유분감이 높은 수분크림과 알로에 성분의 팩 케어가 도움이 돼요.\n\n"
                + "더 궁금한 점이 있으면 편하게 물어봐 주세요!";
    }
}

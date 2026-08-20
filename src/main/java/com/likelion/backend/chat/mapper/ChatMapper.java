package com.likelion.backend.chat.mapper;

import com.likelion.backend.chat.dto.GeneratedConversationId;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {
    Integer findConversationId(@Param("userId") int userId);
    void insertConversation(@Param("userId") int userId, @Param("holder") GeneratedConversationId holder);
    void insertMessage(@Param("conversationId") int conversationId,
                       @Param("senderType") String senderType,
                       @Param("messageContent") String messageContent);
}

package com.likelion.backend.chat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @Min(value = 1, message = "userId 가 필요하다")
    private int userId;

    /** CHATBOT_MESSAGE.message_content 가 varchar(500) 이다. */
    @NotBlank(message = "메시지가 필요하다")
    @Size(max = 500, message = "메시지는 500자 이내여야 한다")
    private String message;
}

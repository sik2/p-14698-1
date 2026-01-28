package com.back.global;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfig {

    @Bean
    @Profile("default")
    public ChatClient openaiChatClient(ChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .build();
    }

    @Bean
    @Profile("test")
    public ChatClient ollamaChatClient(ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .build();
    }
}

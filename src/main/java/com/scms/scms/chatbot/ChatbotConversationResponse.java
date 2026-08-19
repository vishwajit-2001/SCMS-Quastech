package com.scms.scms.chatbot;

import java.util.List;

public record ChatbotConversationResponse(
        String role,
        String title,
        String subtitle,
        String greeting,
        List<String> suggestions,
        List<ChatbotMessageView> messages
) {}

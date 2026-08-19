package com.scms.scms.chatbot;

public record ChatbotMessageView(
        String sender,
        String text,
        String timestamp
) {}

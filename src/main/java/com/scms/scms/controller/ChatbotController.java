package com.scms.scms.controller;

import com.scms.scms.chatbot.ChatbotConversationResponse;
import com.scms.scms.chatbot.ChatbotMessageRequest;
import com.scms.scms.chatbot.ChatbotMessageView;
import com.scms.scms.service.chatbot.ChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatbotController {

    private static final String HISTORY_KEY = "chatbotHistory";

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/api/chatbot/context")
    public ResponseEntity<ChatbotConversationResponse> context(HttpSession session) {
        SessionUser user = resolveUser(session);
        if (!user.loggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ChatbotMessageView> history = getHistory(session);
        if (history.isEmpty()) {
            ChatbotConversationResponse response = chatbotService.buildContext(user.role(), user.email(), history);
            history.add(new ChatbotMessageView("assistant", response.greeting(), currentLabel()));
            saveHistory(session, history);
            return ResponseEntity.ok(new ChatbotConversationResponse(
                    response.role(),
                    response.title(),
                    response.subtitle(),
                    response.greeting(),
                    response.suggestions(),
                    history
            ));
        }
        ChatbotConversationResponse response = chatbotService.buildContext(user.role(), user.email(), history);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/chatbot/message")
    public ResponseEntity<ChatbotConversationResponse> message(@RequestBody ChatbotMessageRequest request, HttpSession session) {
        SessionUser user = resolveUser(session);
        if (!user.loggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String message = request != null ? request.message() : "";
        List<ChatbotMessageView> history = getHistory(session);
        ChatbotConversationResponse response = chatbotService.handleMessage(user.role(), user.email(), message, history);
        saveHistory(session, response.messages());
        return ResponseEntity.ok(response);
    }

    private SessionUser resolveUser(HttpSession session) {
        if (session == null) {
            return SessionUser.missing();
        }
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || role == null) {
            return SessionUser.missing();
        }
        if (!"STUDENT".equalsIgnoreCase(role) && !"TEACHER".equalsIgnoreCase(role)) {
            return SessionUser.missing();
        }
        return new SessionUser(true, role.toUpperCase(), email);
    }

    @SuppressWarnings("unchecked")
    private List<ChatbotMessageView> getHistory(HttpSession session) {
        Object value = session.getAttribute(HISTORY_KEY);
        if (value instanceof List<?> list) {
            List<ChatbotMessageView> copy = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ChatbotMessageView view) {
                    copy.add(view);
                }
            }
            return copy;
        }
        return new ArrayList<>();
    }

    private void saveHistory(HttpSession session, List<ChatbotMessageView> history) {
        session.setAttribute(HISTORY_KEY, new ArrayList<>(history));
    }

    private String currentLabel() {
        return java.time.LocalDate.now().toString();
    }

    private record SessionUser(boolean loggedIn, String role, String email) {
        static SessionUser missing() {
            return new SessionUser(false, null, null);
        }
    }
}

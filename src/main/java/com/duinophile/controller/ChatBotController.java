package com.duinophile.controller;

import com.duinophile.service.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    private static final int MAX_MESSAGE_LENGTH = 5000;

    // ── Page ──────────────────────────────────────────────────────────────

    @GetMapping("/chatbot")
    public String renderChatbotPage() {
        return "chatbot";
    }

    // ── API ───────────────────────────────────────────────────────────────

    @PostMapping("/api/chatbot/ask")
    @ResponseBody
    public ResponseEntity<Map<String, String>> askChatBot(@RequestBody Map<String, String> payload) {
        String message = payload.getOrDefault("message", "").trim();

        // Input validation
        if (message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("reply", "Please enter a message or paste your Arduino code before sending."));
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("reply", "Message is too long. Please limit your input to "
                            + MAX_MESSAGE_LENGTH + " characters."));
        }

        ChatBotService.ChatResponse response = chatBotService.getBotResponse(message);

        String explanation = response.getExplanation();
        if (explanation != null) {
            // Strip trailing "Ex" or "Ex Fixed code..." hallucinations from code model
            explanation = explanation.replaceAll("(?i)\\bEx\\b\\s*(Fixed code:?)?.*$", "").trim();
        }

        String fixedCode = response.getFixed_code();
        String formattedReply;

        if (fixedCode != null && !fixedCode.isBlank()) {
            // Strip leading "Fixed code:" if the model generated it
            fixedCode = fixedCode.replaceFirst("(?i)^Fixed code:?\\s*", "").trim();
            formattedReply = "💡 " + explanation + "\n\n💻 Corrected Code:\n" + fixedCode;
        } else {
            formattedReply = "💡 " + explanation;
        }

        Map<String, String> finalResponse = new HashMap<>();
        finalResponse.put("fixed_code",    fixedCode   != null ? fixedCode   : "");
        finalResponse.put("explanation",   explanation != null ? explanation : "");
        finalResponse.put("original_code", message);
        finalResponse.put("reply",         formattedReply);

        return ResponseEntity.ok(finalResponse);
    }
}

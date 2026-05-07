package com.duinophile.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class ChatBotService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_CODE_API_URL = "http://localhost:8000/api/chat";
    private final String PYTHON_PDF_API_URL = "http://localhost:5000/get";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatRequest {
        private String instruction;
        private String code;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatResponse {
        private String fixed_code;
        private String explanation;
    }

    public boolean isCodeQuery(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase();
        
        String[] codeKeywords = {
            "void setup()", "void loop()", "digitalwrite", "pinmode", 
            "serial.print", "#include <", "int ", "delay(", "};"
        };
        
        int matchCount = 0;
        for (String kw : codeKeywords) {
            if (lower.contains(kw)) {
                matchCount++;
            }
        }
        
        long semicolonCount = text.chars().filter(ch -> ch == ';').count();
        long curlyCount = text.chars().filter(ch -> ch == '{' || ch == '}').count();

        return matchCount >= 1 || (semicolonCount >= 2 && curlyCount >= 2);
    }

    public ChatResponse getBotResponse(String message) {
        try {
            if (isCodeQuery(message)) {
                // Route to Arduino Code ML Model
                ChatRequest req = new ChatRequest("Fix this code:", message);
                return restTemplate.postForObject(PYTHON_CODE_API_URL, req, ChatResponse.class);
            } else {
                // Route to PDF Knowledge Model
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                
                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                map.add("msg", message);
                
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
                
                String pdfResponse = restTemplate.postForObject(PYTHON_PDF_API_URL, request, String.class);
                return new ChatResponse(null, pdfResponse);
            }
        } catch (Exception e) {
            System.err.println("[Duino Bot] Backend connection failed: " + e.getMessage() + ". The models might still be starting up in the background.");
            return new ChatResponse("", "I am still waking up! My AI models are currently starting in the background. Please give me a moment and try your message again.");
        }
    }
}

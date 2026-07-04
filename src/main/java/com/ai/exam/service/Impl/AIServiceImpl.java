package com.ai.exam.service.Impl;
import com.ai.exam.dto.ai.ChatResponse;
import com.ai.exam.dto.ai.ChatMessage;
import com.ai.exam.dto.ai.ChatRequest;
import com.ai.exam.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import java.util.List;

@Service
public class AIServiceImpl implements AIService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${kimi.api.api-key}")
    private String kimiApiKey;
    @Value("${kimi.api.base-url}")
    private String kimiBaseUrl;
    @Value("${kimi.api.model}")
    private String model;
    @Value("${kimi.api.temperature}")
    private Double temperature;
    @Value("${kimi.api.max-tokens}")
    private Integer maxTokens;


    @Override
    public String getChatCompletion(List<ChatMessage> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(kimiApiKey);

            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            String apiUrl = kimiBaseUrl + "/chat/completions";
            HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, headers);
            ChatResponse response = restTemplate.postForObject(apiUrl, httpEntity, ChatResponse.class);

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent(); // 返回第一个选项的消息内容
            }
                return "AI服务暂不可用，请稍后再试。";

        } catch (RestClientException e) {
            System.err.println("AI服务调用失败: " + e.getMessage());
            return "AI服务暂时不可用，请稍后再试。";
        }
    }
}

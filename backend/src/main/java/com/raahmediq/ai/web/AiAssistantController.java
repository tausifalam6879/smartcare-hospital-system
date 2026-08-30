package com.raahmediq.ai.web;

import com.raahmediq.ai.service.AiAssistantService;
import com.raahmediq.ai.web.AiDtos.AskRequest;
import com.raahmediq.ai.web.AiDtos.AskResponse;
import com.raahmediq.ai.web.AiDtos.AssistantStatusResponse;
import com.raahmediq.ai.web.AiDtos.ConversationResponse;
import com.raahmediq.ai.web.AiDtos.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasRole('PATIENT')")
public class AiAssistantController {
    private final AiAssistantService service;

    public AiAssistantController(AiAssistantService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public AssistantStatusResponse status() {
        return service.status();
    }

    @PostMapping("/conversations")
    public ConversationResponse create(@AuthenticationPrincipal Jwt jwt) {
        return service.createConversation(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(@AuthenticationPrincipal Jwt jwt) {
        return service.conversations(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
        return service.messages(UUID.fromString(jwt.getSubject()), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public AskResponse ask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId,
                           @Valid @RequestBody AskRequest request) {
        return service.ask(UUID.fromString(jwt.getSubject()), conversationId, request.question());
    }
}

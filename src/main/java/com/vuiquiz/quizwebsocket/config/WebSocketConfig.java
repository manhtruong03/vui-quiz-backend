package com.vuiquiz.quizwebsocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // For messages TO clients (broadcast or specific)
        // Tell the simple broker to handle destinations prefixed with /topic AND /queue (or /user)
        // When using convertAndSendToUser("/queue/private", ...),
        // Spring resolves this to a user-specific queue like "/user/{username}/queue/private"
        // or "/queue/private-user<session_id>" if no username.
        // The simple broker needs to be aware of these top-level prefixes.
        config.enableSimpleBroker("/topic", "/queue"); // <--- MODIFIED HERE

        // For messages FROM clients to @MessageMapping methods in controllers
        config.setApplicationDestinationPrefixes("/app");

        // Optional: If you want to customize how user destinations are handled
        // By default, SimpMessagingTemplate prepends "/user/" to the user name (or session id if no authenticated user)
        // and sends to that destination. Your client subscribes to /user/queue/private.
        // The broker needs to match these.
         config.setUserDestinationPrefix("/user"); // This is usually the default
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket handshake endpoint
        registry.addEndpoint("/ws-quiz")
                .setAllowedOrigins("*"); // Adjust in production
    }
}

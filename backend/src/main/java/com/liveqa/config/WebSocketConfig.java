package com.liveqa.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 *
 * WebSocketおよびSTOMP通信の設定をおこなうクラス。
 *
 * @author mizumura
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> corsAllowedOrigins;

    /**
     * WebSocket接続を許可するフロントエンドURLを受け取る。
     *
     * @param corsAllowedOrigins WebSocket接続を許可するオリジン一覧
     */
    public WebSocketConfig(
            @Value("${app.cors.allowed-origins}") List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    /**
     * WebSocket接続用のSTOMPエンドポイントを登録する。
     * HTTPセッションの属性をWebSocketセッションへ引き継ぎ、
     * フロントエンドからの接続を許可する。
     *
     * @param registry STOMPエンドポイントの設定用オブジェクト
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins(corsAllowedOrigins.toArray(String[]::new));
    }

    /**
     * STOMPメッセージを配信するsimple brokerを設定する。
     *
     * @param registry メッセージブローカーの設定用オブジェクト
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

}

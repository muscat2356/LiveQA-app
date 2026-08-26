package com.liveqa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * 
 * WebSocketおよびSTOMP通信の設定をおこなうクラス。
 * @author mizumura
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
                .setAllowedOrigins("http://localhost:3000");
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

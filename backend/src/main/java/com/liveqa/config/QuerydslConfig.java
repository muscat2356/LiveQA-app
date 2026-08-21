package com.liveqa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * QueryDSL の入口（JPAQueryFactory）をDIする設定
 * QuerydslConfig
 * @author ichikura
 */
@Configuration
public class QuerydslConfig {

    /**
     * Hibernate 経由で SELECT を発行する工場を登録する
     * @param entityManager 永続化コンテキスト
     * @return QueryDSL のクエリ入口
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

}

package com.liveqa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session + Redis。
 * Cookie 名 SESSION / SameSite 等は本番方針に合わせる。
 * @author ichikura
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "live-qa")
public class SessionConfig {

    private final boolean cookieSecure;

    public SessionConfig(@Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseSecureCookie(this.cookieSecure);
        serializer.setSameSite("Lax");
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}

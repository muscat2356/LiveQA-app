package com.liveqa.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.liveqa.dto.response.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import jakarta.servlet.http.HttpSession;

/**
 * springdoc の表示設定。
 * 契約の正は docs/api/openapi-notes.md。ここは実装からの閲覧用。
 * OpenApiConfig
 * @author ichikura
 */
@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig()
                .addRequestWrapperToIgnore(OidcUser.class, HttpSession.class);
    }

    /**
     * OpenAPI の表紙と Cookie 認証の説明を発行するメソッド
     * @return OpenAPI 定義
     */
    @Bean
    public OpenAPI liveQaOpenAPI() {
        final Components components = new Components()
                .addSecuritySchemes("SESSION", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("SESSION")
                        .description("Spring Session の Cookie。oauth2Login 成功後に付く。"));
        ModelConverters.getInstance().readAll(ErrorResponse.class)
                .forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("live-qa-app API")
                        .version("0.0.1")
                        .description("""
                                契約の正は docs/api/openapi-notes.md。Swagger は実装からの閲覧用。
                                矛盾したら先に設計メモを直し、実装を合わせる。

                                認証: Cookie `SESSION`（Keycloak OIDC → Spring Session + Redis）。
                                状態変更（POST / PATCH / DELETE）はヘッダ `X-XSRF-TOKEN` が必要（Cookie `XSRF-TOKEN` と突合）。
                                例外: `POST /api/auth/guest` は CSRF 検証しない（応答で `XSRF-TOKEN` が載る）。

                                Try it out はブラウザから同一オリジンで Cookie を送る。Swagger UI だけでは CSRF ヘッダは自動では付かない。

                                STOMP（`/topic/events/{eventId}/questions` など）は springdoc の外。openapi-notes の STOMP 節を見る。
                                """))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList("SESSION"));
    }
}

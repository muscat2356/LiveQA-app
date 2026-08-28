package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;

class SessionConfigTest {

    @Test
    void デフォルトはSetCookieにSecureが無い() {
        final String header = setCookieHeader(false);

        assertThat(header).contains("SESSION=");
        assertThat(header).doesNotContain("Secure");
    }

    @Test
    void trueならSetCookieにSecureがある() {
        final String header = setCookieHeader(true);

        assertThat(header).contains("SESSION=");
        assertThat(header).contains("Secure");
    }

    private static String setCookieHeader(boolean cookieSecure) {
        final CookieSerializer serializer = new SessionConfig(cookieSecure).cookieSerializer();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "test-session"));
        return response.getHeader("Set-Cookie");
    }
}

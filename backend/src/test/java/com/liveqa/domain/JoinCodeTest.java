package com.liveqa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JoinCodeTest {

    @Test
    void generate_8文字の英数字() {
        final JoinCode code = JoinCode.generate();

        assertThat(code.code()).hasSize(8);
        assertThat(code.code()).matches("^[A-Za-z0-9]{8}$");
    }

    @Test
    void form_正常なら同じ文字列() {
        assertThat(JoinCode.form("Ab12Cd34").code()).isEqualTo("Ab12Cd34");
    }

    @Test
    void form_桁数が違えば例外() {
        assertThatThrownBy(() -> JoinCode.form("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不正");
    }

    @Test
    void form_不正文字なら例外() {
        assertThatThrownBy(() -> JoinCode.form("Ab12Cd3!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

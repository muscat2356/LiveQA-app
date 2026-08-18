package com.liveqa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VoterKeyTest {

    @Test
    void guest_はg接頭辞() {
        assertThat(VoterKey.guest("abc").value()).isEqualTo("g:abc");
    }

    @Test
    void host_はu接頭辞() {
        assertThat(VoterKey.host(1L).value()).isEqualTo("u:1");
    }
}

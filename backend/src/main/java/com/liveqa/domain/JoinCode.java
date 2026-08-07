package com.liveqa.domain;

import java.security.SecureRandom;
import java.util.Random;

public record JoinCode(String code) {

    private static final String POOL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789";

    private static final int CODE_LENGTH = 8;

    private static final Random RANDOM = new SecureRandom();

    public JoinCode {
        if (code.length() != CODE_LENGTH || !code.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("不正な値です。");
        }
    }

    public static JoinCode generate() {
        StringBuilder sd = new StringBuilder(CODE_LENGTH);

        for(int i = 0; i < CODE_LENGTH; i++) {
            sd.append(POOL.charAt(RANDOM.nextInt(POOL.length())));
        }

        return new JoinCode(sd.toString());
    }

    public static JoinCode form(String joinCode) {
        return new JoinCode(joinCode);
    }
}

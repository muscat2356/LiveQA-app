package com.liveqa.domain;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 8文字の参加コード（値オブジェクト）
 * JoinCode
 * @author ichikura
 */
public record JoinCode(String code) {

    private static final String POOL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789";

    private static final int CODE_LENGTH = 8;

    private static final Random RANDOM = new SecureRandom();

    /**
     * 8文字英数字であることを検証する
     * 長さ不正・null・英数字以外はIllegalArgumentException
     */
    public JoinCode {
        if (code == null || code.length() != CODE_LENGTH || !code.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("不正な値です。");
        }
    }

    /**
     * 参加コードの新規発行を行うメソッド
     * @return 発行したJoinCode
     */
    public static JoinCode generate() {
        StringBuilder sd = new StringBuilder(CODE_LENGTH);

        for(int i = 0; i < CODE_LENGTH; i++) {
            sd.append(POOL.charAt(RANDOM.nextInt(POOL.length())));
        }

        return new JoinCode(sd.toString());
    }

    /**
     * 文字列から参加コードの復元を行うメソッド
     * 検証はcompact constructor
     * @param joinCode 参加コード文字列
     * @return 復元したJoinCode
     */
    public static JoinCode form(String joinCode) {
        return new JoinCode(joinCode);
    }
}

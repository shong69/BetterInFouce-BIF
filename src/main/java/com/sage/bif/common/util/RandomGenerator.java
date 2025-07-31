package com.sage.bif.common.util;

import java.util.Random;

public class RandomGenerator {

    private static final String NICKNAME_PREFIX = "사용자";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String CONNECTION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new Random();

    public static String generateNickname() {
        StringBuilder sb = new StringBuilder(NICKNAME_PREFIX);

        for (int i = 0; i < 6; i++) {
            sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        return sb.toString();
    }

    public static String generateConnectionCode() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(CONNECTION_CODE_CHARS.charAt(random.nextInt(CONNECTION_CODE_CHARS.length())));
        }

        return sb.toString();
    }

    public static String generateUniqueNickname(NicknameChecker checker) {
        String nickname;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            nickname = generateNickname();
            attempts++;
        } while (checker.isNicknameExists(nickname) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new RuntimeException("고유한 닉네임을 생성할 수 없습니다.");
        }

        return nickname;
    }

    public static String generateUniqueConnectionCode(ConnectionCodeChecker checker) {
        String connectionCode;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            connectionCode = generateConnectionCode();
            attempts++;
        } while (checker.isConnectionCodeExists(connectionCode) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new RuntimeException("고유한 연결 코드를 생성할 수 없습니다.");
        }

        return connectionCode;
    }

    @FunctionalInterface
    public interface NicknameChecker {
        boolean isNicknameExists(String nickname);
    }

    @FunctionalInterface
    public interface ConnectionCodeChecker {
        boolean isConnectionCodeExists(String connectionCode);
    }
}
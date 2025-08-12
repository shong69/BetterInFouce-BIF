package com.sage.bif.common.util;

import com.sage.bif.common.exception.BaseException;
import com.sage.bif.common.exception.ErrorCode;

import java.util.Random;

public class RandomGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String CONNECTION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new Random();

    public static String generateNickname(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);

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

    public static String generateUniqueNickname(String prefix, NicknameChecker checker) {
        String nickname;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            nickname = generateNickname(prefix);
            attempts++;
        } while (checker.isNicknameExists(nickname) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new BaseException(ErrorCode.AUTH_NICKNAME_DUPLICATE);
        }

        return nickname;
    }

    public static String generateUniqueConnectionCode(ConnectionCodeChecker checker) {
        String connectionCode;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            connectionCode = generateConnectionCode();
            attempts++;
        } while (checker.isConnectionCodeExists(connectionCode) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new BaseException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
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

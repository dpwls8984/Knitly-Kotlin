package com.mysite.knitly.domain.util;

import java.util.Locale;

// 같은 사용자(UUID)면 언제나 같은 별칭을 얻도록 하는 유틸.
// 익명의 털실-1234 형식 (댓글의 순번 규칙은 서비스 로직에서 처리)
public final class Anonymizer {
    private static final String PREFIX = "익명의 털실-";

    private Anonymizer() {}

    public static String yarn(Long userId) {
        if (userId == null) return PREFIX + "0000";
        int hash = Math.abs(userId.toString().toLowerCase(Locale.ROOT).hashCode());
        return PREFIX + String.format("%04d", hash % 10000);
    }
}

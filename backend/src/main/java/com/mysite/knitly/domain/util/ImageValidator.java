package com.mysite.knitly.domain.util;

import java.util.regex.Pattern;

public final class ImageValidator {
    private static final Pattern ALLOWED
            = Pattern.compile("(?i).+\\.(png|jpg|jpeg)(\\?.*)?$");

    private ImageValidator() {}

    public static boolean isAllowedImageUrl(String url) {
        if (url == null || url.isBlank()) return true; // 이미지 미첨부는 통과
        return ALLOWED.matcher(url).matches();
    }
}

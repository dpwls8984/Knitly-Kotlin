package com.mysite.knitly.domain.design.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalFileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("로컬 저장 - URL 반환 및 URL→실경로 복원 성공")
    void savePdfFile_and_resolve_ok() throws Exception {
        // given
        LocalFileStorage storage = new LocalFileStorage();
        setField(storage, "uploadDir", tempDir.toString());
        setField(storage, "publicPrefix", "/files");

        byte[] pdfBytes = new byte[]{1,2,3};

        // when
        String url = storage.savePdfFile(pdfBytes);

        // then
        assertThat(url).startsWith("/files/");

        // URL -> 실제 경로 복원
        Path abs = storage.toAbsolutePathFromUrl(url);
        assertThat(Files.exists(abs)).isTrue();
        assertThat(Files.size(abs)).isEqualTo(3);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}

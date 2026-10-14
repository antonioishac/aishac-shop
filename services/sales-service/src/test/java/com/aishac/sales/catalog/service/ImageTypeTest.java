package com.aishac.sales.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aishac.sales.catalog.TestImages;
import org.junit.jupiter.api.Test;

/** RN-16: o formato vem do conteúdo, nunca do nome nem do Content-Type. */
class ImageTypeTest {

    @Test
    void shouldDetectSupportedFormatsByMagicNumber() {
        assertThat(ImageType.detect(TestImages.jpeg(16))).contains(ImageType.JPEG);
        assertThat(ImageType.detect(TestImages.png(16))).contains(ImageType.PNG);
        assertThat(ImageType.detect(TestImages.webp(16))).contains(ImageType.WEBP);
    }

    @Test
    void shouldRejectOtherContentAndTruncatedHeaders() {
        assertThat(ImageType.detect(TestImages.text())).isEmpty();
        assertThat(ImageType.detect(new byte[0])).isEmpty();
        assertThat(ImageType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
        // RIFF sem a marca WEBP (ex.: WAV)
        assertThat(ImageType.detect("RIFF0000WAVE".getBytes())).isEmpty();
    }

    @Test
    void shouldExposeContentTypeAndExtension() {
        assertThat(ImageType.PNG.contentType()).isEqualTo("image/png");
        assertThat(ImageType.JPEG.extension()).isEqualTo("jpg");
    }
}

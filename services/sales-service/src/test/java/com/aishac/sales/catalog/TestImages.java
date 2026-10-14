package com.aishac.sales.catalog;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

/** Arquivos mínimos com o magic number de cada formato (o conteúdo depois do cabeçalho é irrelevante). */
public final class TestImages {

    private static final HexFormat HEX = HexFormat.of();

    private TestImages() {}

    public static byte[] jpeg(int size) {
        return withHeader(size, "FFD8FFE0");
    }

    public static byte[] png(int size) {
        return withHeader(size, "89504E470D0A1A0A");
    }

    /** {@code RIFF} + tamanho (4 bytes) + {@code WEBP}. */
    public static byte[] webp(int size) {
        return withHeader(size, "52494646" + "00000000" + "57454250");
    }

    public static byte[] text() {
        return "isto não é uma imagem".getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] withHeader(int size, String hexHeader) {
        byte[] header = HEX.parseHex(hexHeader);
        byte[] content = Arrays.copyOf(header, Math.max(size, header.length));
        Arrays.fill(content, header.length, content.length, (byte) 0x2A);
        return content;
    }
}

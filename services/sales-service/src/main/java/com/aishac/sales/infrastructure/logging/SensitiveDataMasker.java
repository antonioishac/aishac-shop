package com.aishac.sales.infrastructure.logging;

import java.util.regex.Pattern;

/**
 * Mascara CPF e e-mail em texto livre. Usado pelo log estruturado como rede de segurança: nenhum dado
 * pessoal deve ser logado, mas, se for, sai mascarado.
 */
public final class SensitiveDataMasker {

    // CPF com ou sem pontuação: 123.456.789-09 ou 12345678909 (não casa com números maiores).
    private static final Pattern CPF = Pattern.compile("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?(\\d{2})(?!\\d)");
    private static final Pattern EMAIL =
            Pattern.compile("([A-Za-z0-9])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private SensitiveDataMasker() {}

    public static String mask(String text) {
        String withoutCpf = CPF.matcher(text).replaceAll("***.***.***-$1");
        return EMAIL.matcher(withoutCpf).replaceAll("$1***@$2");
    }
}

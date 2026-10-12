package com.aishac.customer.customer;

import java.util.concurrent.atomic.AtomicInteger;

/** Gera CPFs válidos e distintos para testes que compartilham o banco. */
public final class CpfFixtures {

    private static final AtomicInteger SEQUENCE = new AtomicInteger((int) (System.nanoTime() % 100_000_000));

    private CpfFixtures() {}

    public static String nextValidCpf() {
        String base = "%09d".formatted(Math.floorMod(SEQUENCE.incrementAndGet(), 1_000_000_000));
        if (base.chars().distinct().count() == 1) {
            return nextValidCpf();
        }
        String withFirst = base + checkDigit(base);
        return withFirst + checkDigit(withFirst);
    }

    private static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            sum += (digits.charAt(i) - '0') * (digits.length() + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}

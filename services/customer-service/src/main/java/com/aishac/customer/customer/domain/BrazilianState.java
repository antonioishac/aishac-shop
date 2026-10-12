package com.aishac.customer.customer.domain;

/** Unidades federativas aceitas no endereço (RN-11). */
public enum BrazilianState {
    AC,
    AL,
    AP,
    AM,
    BA,
    CE,
    DF,
    ES,
    GO,
    MA,
    MT,
    MS,
    MG,
    PA,
    PB,
    PR,
    PE,
    PI,
    RJ,
    RN,
    RS,
    RO,
    RR,
    SC,
    SP,
    SE,
    TO;

    public static BrazilianState of(String code) {
        try {
            return valueOf(code.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw CustomerException.invalidField("state", "must be a Brazilian state code");
        }
    }
}

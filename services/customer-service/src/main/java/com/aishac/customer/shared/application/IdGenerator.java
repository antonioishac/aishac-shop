package com.aishac.customer.shared.application;

import java.util.UUID;

/** Gera identificadores de agregados e entidades (UUID v7, ordenável no tempo). */
public interface IdGenerator {

    UUID newId();
}

package com.aishac.customer.customer.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Agregado raiz do contexto Clientes. Concentra as regras de status (RN-05 a RN-08) e de endereços (RN-12 a
 * RN-15). O estado só muda pelos métodos de negócio; a {@code version} é controlada pela persistência.
 */
@Getter
public class Customer {

    public static final int MAX_ADDRESSES = 10;
    private static final int MIN_NAME = 3;
    private static final int MAX_NAME = 150;
    private static final int MIN_REASON = 3;
    private static final int MAX_REASON = 255;

    /** Mais recente primeiro; empate no instante resolvido pelo id (UUID v7 é ordenado no tempo). */
    private static final Comparator<Address> NEWEST_FIRST = Comparator.comparing(Address::getCreatedAt)
            .thenComparing(Address::getId)
            .reversed();

    private final UUID id;
    private String fullName;
    private final Cpf cpf;
    private Email email;
    private @Nullable Phone phone;
    private CustomerStatus status;
    private @Nullable String blockReason;
    private @Nullable Instant blockedAt;
    private @Nullable Instant deactivatedAt;
    private final List<Address> addresses;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Customer(
            UUID id,
            String fullName,
            Cpf cpf,
            Email email,
            @Nullable Phone phone,
            CustomerStatus status,
            @Nullable String blockReason,
            @Nullable Instant blockedAt,
            @Nullable Instant deactivatedAt,
            List<Address> addresses,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.cpf = cpf;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.blockReason = blockReason;
        this.blockedAt = blockedAt;
        this.deactivatedAt = deactivatedAt;
        this.addresses = new ArrayList<>(addresses);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cadastro de um novo cliente: nasce ACTIVE e sem endereços (RN-05). */
    public static Customer register(
            UUID id, String fullName, Cpf cpf, Email email, @Nullable Phone phone, Instant now) {
        return new Customer(
                id,
                validName(fullName),
                cpf,
                email,
                phone,
                CustomerStatus.ACTIVE,
                null,
                null,
                null,
                List.of(),
                0,
                now,
                now);
    }

    /** Reconstitui um cliente já persistido, sem aplicar regras de criação. */
    public static Customer restore(
            UUID id,
            String fullName,
            Cpf cpf,
            Email email,
            @Nullable Phone phone,
            CustomerStatus status,
            @Nullable String blockReason,
            @Nullable Instant blockedAt,
            @Nullable Instant deactivatedAt,
            List<Address> addresses,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new Customer(
                id,
                fullName,
                cpf,
                email,
                phone,
                status,
                blockReason,
                blockedAt,
                deactivatedAt,
                addresses,
                version,
                createdAt,
                updatedAt);
    }

    // --- Dados cadastrais e status --------------------------------------------------------------

    /** Altera nome, e-mail e telefone. O CPF nunca muda (RN-03). */
    public void updateDetails(String newFullName, Email newEmail, @Nullable Phone newPhone, Instant now) {
        ensureNotInactive();
        this.fullName = validName(newFullName);
        this.email = newEmail;
        this.phone = newPhone;
        this.updatedAt = now;
    }

    public void block(String reason, Instant now) {
        String validReason = reason.strip();
        if (validReason.length() < MIN_REASON || validReason.length() > MAX_REASON) {
            throw CustomerException.invalidField(
                    "reason", "must have between " + MIN_REASON + " and " + MAX_REASON + " characters");
        }
        transitionTo(CustomerStatus.BLOCKED, now);
        this.blockReason = validReason;
        this.blockedAt = now;
    }

    public void unblock(Instant now) {
        transitionTo(CustomerStatus.ACTIVE, now);
        clearBlock();
    }

    /** Encerra o cadastro sem excluir (RN-07, ADR-016). */
    public void deactivate(Instant now) {
        transitionTo(CustomerStatus.INACTIVE, now);
        clearBlock();
        this.deactivatedAt = now;
    }

    // --- Endereços --------------------------------------------------------------------------------

    /** Inclui um endereço; o primeiro vira padrão (RN-13). Máximo de {@value #MAX_ADDRESSES} (RN-12). */
    public Address addAddress(UUID addressId, AddressData data, Instant now) {
        ensureNotInactive();
        if (addresses.size() >= MAX_ADDRESSES) {
            throw CustomerException.addressLimitExceeded(MAX_ADDRESSES);
        }
        Address address = Address.create(addressId, data, addresses.isEmpty(), now);
        addresses.add(address);
        this.updatedAt = now;
        return address;
    }

    public Address updateAddress(UUID addressId, AddressData data, Instant now) {
        ensureNotInactive();
        Address address = address(addressId);
        address.update(data, now);
        this.updatedAt = now;
        return address;
    }

    /** Remove o endereço; se era o padrão, o mais recente dos restantes assume (RN-15). */
    public void removeAddress(UUID addressId, Instant now) {
        ensureNotInactive();
        Address removed = address(addressId);
        addresses.remove(removed);
        if (removed.isDefault()) {
            addresses.stream().min(NEWEST_FIRST).ifPresent(newest -> newest.setDefault(true, now));
        }
        this.updatedAt = now;
    }

    /** Define o padrão e desmarca o anterior na mesma operação (RN-14). Idempotente. */
    public void makeDefault(UUID addressId, Instant now) {
        ensureNotInactive();
        Address target = address(addressId);
        addresses.forEach(address -> address.setDefault(address == target, now));
        this.updatedAt = now;
    }

    public Address address(UUID addressId) {
        return addresses.stream()
                .filter(address -> address.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> CustomerException.addressNotFound(addressId));
    }

    public Optional<Address> defaultAddress() {
        return addresses.stream().filter(Address::isDefault).findFirst();
    }

    /** Endereços com o padrão primeiro e os demais na ordem de cadastro. */
    public List<Address> getAddresses() {
        return addresses.stream()
                .sorted(Comparator.comparing((Address address) -> !address.isDefault())
                        .thenComparing(Address::getCreatedAt)
                        .thenComparing(Address::getId))
                .toList();
    }

    // --- Regras internas --------------------------------------------------------------------------

    private void transitionTo(CustomerStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw CustomerException.invalidStatusTransition(status, target);
        }
        this.status = target;
        this.updatedAt = now;
    }

    private void clearBlock() {
        this.blockReason = null;
        this.blockedAt = null;
    }

    private void ensureNotInactive() {
        if (status == CustomerStatus.INACTIVE) {
            throw CustomerException.customerInactive();
        }
    }

    private static String validName(String fullName) {
        String name = fullName.strip();
        if (name.length() < MIN_NAME || name.length() > MAX_NAME) {
            throw CustomerException.invalidField(
                    "fullName", "must have between " + MIN_NAME + " and " + MAX_NAME + " characters");
        }
        return name;
    }
}

package com.beamcard.user.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.persistence.model.UserJpa;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = Mappers.getMapper(UserPersistenceMapper.class);

    @Test
    void toDomainMapsEveryField() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-02-01T00:00:00Z");
        UserJpa jpa = UserJpa.builder()
                .id(id)
                .email("alice@example.com")
                .passwordHash("$2a$12$hash")
                .googleSub("google-123")
                .plan(UserSubscriptionPlan.PREMIUM)
                .status(UserStatus.SUSPENDED)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        User domain = mapper.toDomain(jpa);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getEmail()).isEqualTo("alice@example.com");
        assertThat(domain.getPasswordHash()).isEqualTo("$2a$12$hash");
        assertThat(domain.getGoogleSub()).isEqualTo("google-123");
        assertThat(domain.getPlan()).isEqualTo(UserSubscriptionPlan.PREMIUM);
        assertThat(domain.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(domain.getCreatedAt()).isEqualTo(created);
        assertThat(domain.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toJpaMapsEveryField() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-02-01T00:00:00Z");
        User domain = User.builder()
                .id(id)
                .email("bob@example.com")
                .passwordHash("$2a$12$other")
                .googleSub("google-456")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        UserJpa jpa = mapper.toJpa(domain);

        assertThat(jpa.getId()).isEqualTo(id);
        assertThat(jpa.getEmail()).isEqualTo("bob@example.com");
        assertThat(jpa.getPasswordHash()).isEqualTo("$2a$12$other");
        assertThat(jpa.getGoogleSub()).isEqualTo("google-456");
        assertThat(jpa.getPlan()).isEqualTo(UserSubscriptionPlan.FREE);
        assertThat(jpa.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(jpa.getCreatedAt()).isEqualTo(created);
        assertThat(jpa.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void roundTripPreservesValues() {
        User original = User.builder()
                .id(UUID.randomUUID())
                .email("carol@example.com")
                .plan(UserSubscriptionPlan.PREMIUM)
                .status(UserStatus.ACTIVE)
                .build();

        User roundTripped = mapper.toDomain(mapper.toJpa(original));

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getEmail()).isEqualTo(original.getEmail());
        assertThat(roundTripped.getPlan()).isEqualTo(original.getPlan());
        assertThat(roundTripped.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toJpa(null)).isNull();
    }
}

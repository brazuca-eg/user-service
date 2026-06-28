package com.beamcard.user.persistence.mapper;

import com.beamcard.user.auth.model.PasswordResetToken;
import com.beamcard.user.persistence.model.PasswordResetTokenJpa;
import org.mapstruct.Mapper;

@Mapper
public interface PasswordResetTokenPersistenceMapper {

    PasswordResetToken toDomain(PasswordResetTokenJpa jpa);

    PasswordResetTokenJpa toJpa(PasswordResetToken token);
}

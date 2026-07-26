package com.beamcard.user.persistence.mapper;

import com.beamcard.user.auth.model.EmailVerificationToken;
import com.beamcard.user.persistence.model.EmailVerificationTokenJpa;
import org.mapstruct.Mapper;

@Mapper
public interface EmailVerificationTokenPersistenceMapper {

    EmailVerificationToken toDomain(EmailVerificationTokenJpa jpa);

    EmailVerificationTokenJpa toJpa(EmailVerificationToken token);
}

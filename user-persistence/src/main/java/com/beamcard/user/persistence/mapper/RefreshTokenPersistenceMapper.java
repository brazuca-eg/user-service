package com.beamcard.user.persistence.mapper;

import com.beamcard.user.auth.model.RefreshToken;
import com.beamcard.user.persistence.model.RefreshTokenJpa;
import org.mapstruct.Mapper;

@Mapper
public interface RefreshTokenPersistenceMapper {

    RefreshToken toDomain(RefreshTokenJpa jpa);

    RefreshTokenJpa toJpa(RefreshToken token);
}

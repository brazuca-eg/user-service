package com.beamcard.user.persistence.mapper;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.persistence.model.UserJpa;
import org.mapstruct.Mapper;

@Mapper
public interface UserPersistenceMapper {

    User toDomain(UserJpa jpa);

    UserJpa toJpa(User user);
}

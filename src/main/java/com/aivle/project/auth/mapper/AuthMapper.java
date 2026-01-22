package com.aivle.project.auth.mapper;

import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {

	@Mapping(target = "role", source = "role")
	SignupResponse toSignupResponse(UserEntity user, RoleName role);
}

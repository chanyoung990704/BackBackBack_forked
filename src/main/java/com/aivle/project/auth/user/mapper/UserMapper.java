package com.aivle.project.auth.user.mapper;

import com.aivle.project.common.util.NameMaskingUtil;
import com.aivle.project.auth.user.dto.UserSummaryDto;
import com.aivle.project.auth.user.entity.RoleName;
import com.aivle.project.auth.user.entity.UserEntity;
import com.aivle.project.auth.user.security.CustomUserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

	// UUID를 문자열 userId로 변환하고, 외부에서 전달받은 역할을 포함한다.
	@Mapping(target = "userId", expression = "java(user.getUuid().toString())")
	@Mapping(target = "name", source = "user.name", qualifiedByName = "maskName")
	@Mapping(target = "role", source = "role")
	UserSummaryDto toSummaryDto(UserEntity user, RoleName role);

	// CustomUserDetails에서도 동일한 userId/role 규칙을 적용한다.
	@Mapping(target = "userId", expression = "java(userDetails.getUuid().toString())")
	@Mapping(target = "email", source = "userDetails.username")
	@Mapping(target = "name", source = "userDetails.name", qualifiedByName = "maskName")
	@Mapping(target = "role", source = "role")
	UserSummaryDto toSummaryDto(CustomUserDetails userDetails, RoleName role);

	@Named("maskName")
	default String maskName(String name) {
		return NameMaskingUtil.mask(name);
	}
}

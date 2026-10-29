package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import com.innowise.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CardMapper.class)
public interface UserMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "cards", ignore = true)
  User toEntity(UserCreateRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "cards", ignore = true)
  void updateEntity(UserUpdateRequest request, @MappingTarget User user);

  UserResponse toResponse(User user);

  UserWithCardsResponse toResponseWithCards(User user);
}

package by.assxmblxr.userservice.mapper.impl;

import by.assxmblxr.userservice.dto.UserCreateRequest;
import by.assxmblxr.userservice.dto.UserResponse;
import by.assxmblxr.userservice.entity.User;
import by.assxmblxr.userservice.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {
  @Override
  public User toEntity(UserCreateRequest request) {
    return User.builder()
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .build();
  }

  @Override
  public UserResponse toResponse(User user) {
    return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .createdAt(user.getCreatedAt())
            .build();
  }
}

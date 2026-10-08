package by.assxmblxr.userservice.mapper;

import by.assxmblxr.userservice.dto.UserCreateRequest;
import by.assxmblxr.userservice.dto.UserResponse;
import by.assxmblxr.userservice.entity.User;

public interface UserMapper {
  User toEntity(UserCreateRequest request);
  UserResponse toResponse(User user);
}

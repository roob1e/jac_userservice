package by.assxmblxr.userservice.service;

import by.assxmblxr.userservice.dto.UserCreateRequest;
import by.assxmblxr.userservice.dto.UserResponse;
import by.assxmblxr.userservice.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  UserResponse create(UserCreateRequest request);
  UserResponse findById(Long id);
  Page<UserResponse> findAll(String firstName, String lastName, Pageable pageable);
  UserResponse update(Long id, UserUpdateRequest request);
  void setActive(Long id, boolean active);
}

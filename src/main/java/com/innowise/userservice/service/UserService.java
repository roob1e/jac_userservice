package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  UserResponse create(UserCreateRequest request);
  UserWithCardsResponse findById(Long id);
  Page<UserResponse> findAll(String name, String surname, Pageable pageable);
  UserResponse update(Long id, UserUpdateRequest request);
  void setActive(Long id, boolean active);
  void deleteById(Long id);
}

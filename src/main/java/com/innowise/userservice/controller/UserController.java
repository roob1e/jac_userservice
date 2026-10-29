package com.innowise.userservice.controller;

import com.innowise.userservice.dto.StatusUpdateRequest;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    UserResponse response = userService.create(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserWithCardsResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  @GetMapping
  public ResponseEntity<Page<UserResponse>> findAll(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          Pageable pageable
  ) {
    return ResponseEntity.ok(userService.findAll(name, surname, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> update(
          @PathVariable Long id,
          @Valid @RequestBody UserUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.update(id, request));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Void> updateStatus(
          @PathVariable Long id,
          @Valid @RequestBody StatusUpdateRequest request) {
    userService.setActive(id, request.active());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      userService.deleteById(id);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

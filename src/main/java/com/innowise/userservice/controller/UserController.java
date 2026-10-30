package com.innowise.userservice.controller;

import com.innowise.userservice.dto.StatusUpdateRequest;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.innowise.userservice.security.CurrentUser.currentUserId;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    UserResponse response = userService.create(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/me")
  public ResponseEntity<UserWithCardsResponse> findMe(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(userService.findById(currentUserId(jwt)));
  }

  @PutMapping("/me")
  public ResponseEntity<UserResponse> updateMe(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody UserUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.update(currentUserId(jwt), request));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{id}")
  public ResponseEntity<UserWithCardsResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<Page<UserResponse>> findAll(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          Pageable pageable
  ) {
    return ResponseEntity.ok(userService.findAll(name, surname, pageable));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> update(
          @PathVariable Long id,
          @Valid @RequestBody UserUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.update(id, request));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}/status")
  public ResponseEntity<Void> updateStatus(
          @PathVariable Long id,
          @Valid @RequestBody StatusUpdateRequest request) {
    userService.setActive(id, request.active());
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}

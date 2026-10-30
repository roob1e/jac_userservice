package com.innowise.userservice.controller;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.CardUpdateRequest;
import com.innowise.userservice.dto.StatusUpdateRequest;
import com.innowise.userservice.service.CardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static com.innowise.userservice.security.CurrentUser.currentUserId;

@RestController
@RequiredArgsConstructor
public class CardController {
  private final CardService cardService;

  @PostMapping("/api/users/me/cards")
  public ResponseEntity<CardResponse> createMine(
          @AuthenticationPrincipal Jwt jwt,
          @Valid @RequestBody CardCreateRequest cardCreateRequest
  ) {
    return created(cardService.create(currentUserId(jwt), cardCreateRequest));
  }

  @GetMapping("/api/users/me/cards")
  public ResponseEntity<List<CardResponse>> findMine(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(cardService.findByUserId(currentUserId(jwt)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/api/users/{userId}/cards")
  public ResponseEntity<CardResponse> create(
          @Valid @RequestBody CardCreateRequest cardCreateRequest,
          @PathVariable Long userId
  ) {
    return created(cardService.create(userId, cardCreateRequest));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/users/{userId}/cards")
  public ResponseEntity<List<CardResponse>> findByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.findByUserId(userId));
  }

  @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
  @GetMapping("/api/cards/{id}")
  public ResponseEntity<CardResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.findById(id));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/cards")
  public ResponseEntity<Page<CardResponse>> findAll(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          Pageable pageable
  ) {
    return ResponseEntity.ok(cardService.findAll(name, surname, pageable));
  }

  @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
  @PutMapping("/api/cards/{id}")
  public ResponseEntity<CardResponse> update(
          @PathVariable Long id,
          @Valid @RequestBody CardUpdateRequest request
  ) {
    return ResponseEntity.ok(cardService.update(id, request));
  }

  @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
  @PatchMapping("/api/cards/{id}/status")
  public ResponseEntity<Void> updateStatus(
          @PathVariable Long id,
          @Valid @RequestBody StatusUpdateRequest request
          ) {
    cardService.setActive(id, request.active());
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ADMIN') or @cardAccess.isOwner(#id, authentication)")
  @DeleteMapping("/api/cards/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    cardService.delete(id);
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<CardResponse> created(CardResponse response) {
    URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/cards/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }
}

package com.innowise.userservice.controller;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.CardUpdateRequest;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CardController {
  private final CardService cardService;

  @PostMapping("/users/{userId}/cards")
  public ResponseEntity<CardResponse> create(
          @Valid @RequestBody CardCreateRequest cardCreateRequest,
          @PathVariable Long userId
  ) {
    CardResponse response = cardService.create(userId, cardCreateRequest);
    URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/cards/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/users/{userId}/cards")
  public ResponseEntity<List<CardResponse>> findByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.findByUserId(userId));
  }

  @GetMapping("/cards/{id}")
  public ResponseEntity<CardResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.findById(id));
  }

  @GetMapping("/cards")
  public ResponseEntity<Page<CardResponse>> findAll(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) String surname,
          Pageable pageable
  ) {
    return ResponseEntity.ok(cardService.findAll(name, surname, pageable));
  }

  @PutMapping("/cards/{id}")
  public ResponseEntity<CardResponse> update(
          @PathVariable Long id,
          @Valid @RequestBody CardUpdateRequest request
  ) {
    return ResponseEntity.ok(cardService.update(id, request));
  }

  @PatchMapping("/cards/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable Long id) {
    cardService.setActive(id, true);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/cards/{id}/deactivate")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    cardService.setActive(id, false);
    return ResponseEntity.noContent().build();
  }
}

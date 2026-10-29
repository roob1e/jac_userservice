package com.innowise.userservice.service;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.CardUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CardService {
  CardResponse create(Long userId ,CardCreateRequest request);
  CardResponse findById(Long id);
  Page<CardResponse> findAll(String firstName, String lastName, Pageable pageable);
  List<CardResponse> findByUserId(Long userId);
  CardResponse update(Long id, CardUpdateRequest request);
  void setActive(Long id, boolean active);
}

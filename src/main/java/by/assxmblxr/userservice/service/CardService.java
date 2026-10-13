package by.assxmblxr.userservice.service;

import by.assxmblxr.userservice.dto.CardCreateRequest;
import by.assxmblxr.userservice.dto.CardResponse;
import by.assxmblxr.userservice.dto.CardUpdateRequest;
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

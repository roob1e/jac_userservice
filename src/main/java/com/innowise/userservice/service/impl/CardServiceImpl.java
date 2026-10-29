package com.innowise.userservice.service.impl;

import static com.innowise.userservice.specification.CardSpecification.*;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.CardUpdateRequest;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.CardNumberAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
  private static final int MAX_CARDS_PER_USER = 5;

  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final CardMapper cardMapper;
  private final CacheManager cacheManager;

  @Override
  @CacheEvict(cacheNames = "users", key = "#userId")
  @Transactional
  public CardResponse create(Long userId, CardCreateRequest request) {
    User user = findUser(userId);
    if (cardRepository.countByUserId(userId) >= MAX_CARDS_PER_USER) {
      throw new CardLimitExceededException(userId, MAX_CARDS_PER_USER);
    }
    if (cardRepository.existsByNumber(request.number())) {
      throw new CardNumberAlreadyExistsException(request.number());
    }
    Card card = cardMapper.toEntity(request);
    user.addCard(card);
    userRepository.flush();
    return cardMapper.toResponse(card);
  }

  @Override
  public CardResponse findById(Long id) {
    return cardMapper.toResponse(findCard(id));
  }

  @Override
  public Page<CardResponse> findAll(String name, String surname, Pageable pageable) {
    Specification<Card> spec = ownerNameContains(name).and(ownerSurnameContains(surname));
    return cardRepository.findAll(spec, pageable).map(cardMapper::toResponse);
  }

  @Override
  public List<CardResponse> findByUserId(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }
    return cardRepository.findByUserId(userId).stream()
            .map(cardMapper::toResponse)
            .toList();
  }

  @Override
  @CacheEvict(cacheNames = "users", key = "#result.userId()")
  @Transactional
  public CardResponse update(Long id, CardUpdateRequest request) {
    Card card = findCard(id);
    if (cardRepository.existsByNumberAndIdNot(request.number(), id)) {
      throw new CardNumberAlreadyExistsException(request.number());
    }
    cardMapper.updateEntity(request, card);
    return cardMapper.toResponse(card);
  }

  @Override
  @Transactional
  public void setActive(Long id, boolean active) {
    Cache cache = cacheManager.getCache("users");
    if (cardRepository.updateActive(id, active) == 0) {
      throw new CardNotFoundException(id);
    }
    Long userId = cardRepository.findUserIdById(id).orElseThrow(() -> new CardNotFoundException(id));
    if (cache != null) {
      cache.evict(userId);
    }
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = "users", key = "#result.userId()")
  public CardResponse delete(Long id) {
    Card card = findCard(id);
    CardResponse response = cardMapper.toResponse(card);
    card.getUser().removeCard(card);
    return response;
  }

  private User findUser(Long id) {
    return userRepository.findByIdForUpdate(id).orElseThrow(() -> new UserNotFoundException(id));
  }

  private Card findCard(Long id) {
    return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
  }
}

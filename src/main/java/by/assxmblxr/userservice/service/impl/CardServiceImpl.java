package by.assxmblxr.userservice.service.impl;

import static by.assxmblxr.userservice.specification.CardSpecification.*;

import by.assxmblxr.userservice.dto.CardCreateRequest;
import by.assxmblxr.userservice.dto.CardResponse;
import by.assxmblxr.userservice.dto.CardUpdateRequest;
import by.assxmblxr.userservice.entity.Card;
import by.assxmblxr.userservice.entity.User;
import by.assxmblxr.userservice.exception.CardLimitExceededException;
import by.assxmblxr.userservice.exception.CardNotFoundException;
import by.assxmblxr.userservice.exception.CardNumberAlreadyExistsException;
import by.assxmblxr.userservice.exception.UserNotFoundException;
import by.assxmblxr.userservice.mapper.CardMapper;
import by.assxmblxr.userservice.repository.CardRepository;
import by.assxmblxr.userservice.repository.UserRepository;
import by.assxmblxr.userservice.service.CardService;
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
    User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    lockUser(userId);
    if (cardRepository.countActiveByUserId(userId) >= MAX_CARDS_PER_USER) {
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
  public Page<CardResponse> findAll(String firstName, String lastName, Pageable pageable) {
    Specification<Card> spec = ownerFirstNameContains(firstName).and(ownerLastNameContains(lastName));
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
    Card card = findCard(id);
    Long userId = card.getUser().getId();

    if (active && !card.isActive()) {
      lockUser(userId);
      if (cardRepository.countActiveByUserId(userId) >= MAX_CARDS_PER_USER) {
        throw new CardLimitExceededException(userId, MAX_CARDS_PER_USER);
      }
    }

    cardRepository.updateActive(id, active);
    Cache cache = cacheManager.getCache("users");
    if (cache != null) {
      cache.evict(userId);
    }
  }

  private void lockUser(Long userId) {
    userRepository.lockRowForUpdate(userId);
  }

  private Card findCard(Long id) {
    return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
  }
}

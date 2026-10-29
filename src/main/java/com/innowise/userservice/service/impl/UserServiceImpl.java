package com.innowise.userservice.service.impl;

import static com.innowise.userservice.specification.UserSpecification.*;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final CardRepository cardRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException(request.email());
    }
    User user = userMapper.toEntity(request);
    return userMapper.toResponse(userRepository.save(user));
  }

  @Override
  @Cacheable(cacheNames = "users", key = "#id")
  public UserWithCardsResponse findById(Long id) {
    User user = userRepository.findWithCardsById(id).orElseThrow(() -> new UserNotFoundException(id));
    return userMapper.toResponseWithCards(user);
  }

  @Override
  public Page<UserResponse> findAll(String name, String surname, Pageable pageable) {
    Specification<User> spec = nameContains(name).and(surnameContains(surname));
    return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
  }

  @Override
  @CacheEvict(cacheNames = "users", key = "#id")
  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {
    User user = findUser(id);
    if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw new EmailAlreadyExistsException(request.email());
    }
    userMapper.updateEntity(request, user);
    return userMapper.toResponse(user);
  }

  @Override
  @CacheEvict(cacheNames = "users", key = "#id")
  @Transactional
  public void setActive(Long id, boolean active) {
    if (userRepository.updateActive(id, active) == 0) {
      throw new UserNotFoundException(id);
    }
    if (!active) {
      cardRepository.deactivateByUserId(id);
    }
  }

  @Override
  @CacheEvict(cacheNames = "users", key = "#id")
  public void deleteById(Long id) {
    userRepository.delete(findUser(id));
  }

  private User findUser(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}

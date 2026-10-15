package by.assxmblxr.userservice.service.impl;

import static by.assxmblxr.userservice.specification.UserSpecification.*;

import by.assxmblxr.userservice.dto.UserCreateRequest;
import by.assxmblxr.userservice.dto.UserResponse;
import by.assxmblxr.userservice.dto.UserUpdateRequest;
import by.assxmblxr.userservice.dto.UserWithCardsResponse;
import by.assxmblxr.userservice.entity.User;
import by.assxmblxr.userservice.exception.EmailAlreadyExistsException;
import by.assxmblxr.userservice.exception.UserNotFoundException;
import by.assxmblxr.userservice.mapper.UserMapper;
import by.assxmblxr.userservice.repository.CardRepository;
import by.assxmblxr.userservice.repository.UserRepository;
import by.assxmblxr.userservice.service.UserService;
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
  public Page<UserResponse> findAll(String firstName, String lastName, Pageable pageable) {
    Specification<User> spec = firstNameContains(firstName).and(lastNameContains(lastName));
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

  private User findUser(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}

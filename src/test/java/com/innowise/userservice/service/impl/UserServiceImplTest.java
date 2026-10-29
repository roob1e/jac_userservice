package com.innowise.userservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.dto.UserWithCardsResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.mapper.UserMapperImpl;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
  private static final Long USER_ID = 1L;
  private static final String EMAIL = "ivan@mail.com";

  @Mock
  private UserRepository userRepository;

  @Mock
  private CardRepository cardRepository;

  @Spy
  private UserMapper userMapper = new UserMapperImpl();

  @InjectMocks
  private UserServiceImpl userService;

  @Test
  void createSavesUserAndReturnsResponse() {
    UserCreateRequest request = new UserCreateRequest("Ivan", "Petrov", EMAIL);
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(USER_ID);
      return user;
    });

    UserResponse response = userService.create(request);

    assertThat(response.id()).isEqualTo(USER_ID);
    assertThat(response.email()).isEqualTo(EMAIL);
    assertThat(response.firstName()).isEqualTo("Ivan");
    assertThat(response.lastName()).isEqualTo("Petrov");
    assertThat(response.active()).isTrue();
  }

  @Test
  void createThrowsWhenEmailAlreadyExists() {
    UserCreateRequest request = new UserCreateRequest("Ivan", "Petrov", EMAIL);
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(EmailAlreadyExistsException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void findByIdReturnsUser() {
    when(userRepository.findWithCardsById(USER_ID)).thenReturn(Optional.of(user()));

    UserWithCardsResponse response = userService.findById(USER_ID);

    assertThat(response.id()).isEqualTo(USER_ID);
    assertThat(response.email()).isEqualTo(EMAIL);
  }

  @Test
  void findByIdThrowsWhenUserNotFound() {
    when(userRepository.findWithCardsById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.findById(USER_ID))
            .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void findAllReturnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(user()), pageable, 1));

    Page<UserResponse> page = userService.findAll("iva", null, pageable);

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent()).extracting(UserResponse::email).containsExactly(EMAIL);
  }

  @Test
  void updateChangesUserFields() {
    User user = user();
    UserUpdateRequest request = new UserUpdateRequest("Petr", "Ivanov", "petr@mail.com");
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot("petr@mail.com", USER_ID)).thenReturn(false);

    UserResponse response = userService.update(USER_ID, request);

    assertThat(user.getFirstName()).isEqualTo("Petr");
    assertThat(user.getLastName()).isEqualTo("Ivanov");
    assertThat(user.getEmail()).isEqualTo("petr@mail.com");
    assertThat(response.email()).isEqualTo("petr@mail.com");
  }

  @Test
  void updateThrowsWhenUserNotFound() {
    UserUpdateRequest request = new UserUpdateRequest("Petr", "Ivanov", "petr@mail.com");
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.update(USER_ID, request))
            .isInstanceOf(UserNotFoundException.class);
    verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
  }

  @Test
  void updateThrowsWhenEmailTakenByAnotherUser() {
    User user = user();
    UserUpdateRequest request = new UserUpdateRequest("Petr", "Ivanov", "taken@mail.com");
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot("taken@mail.com", USER_ID)).thenReturn(true);

    assertThatThrownBy(() -> userService.update(USER_ID, request))
            .isInstanceOf(EmailAlreadyExistsException.class);
    assertThat(user.getEmail()).isEqualTo(EMAIL);
    assertThat(user.getFirstName()).isEqualTo("Ivan");
  }

  @Test
  void activateDoesNotTouchCards() {
    when(userRepository.updateActive(USER_ID, true)).thenReturn(1);

    userService.setActive(USER_ID, true);

    verify(cardRepository, never()).deactivateByUserId(anyLong());
  }

  @Test
  void deactivateAlsoDeactivatesUserCards() {
    when(userRepository.updateActive(USER_ID, false)).thenReturn(1);

    userService.setActive(USER_ID, false);

    verify(cardRepository).deactivateByUserId(USER_ID);
  }

  @Test
  void setActiveThrowsWhenUserNotFound() {
    when(userRepository.updateActive(USER_ID, false)).thenReturn(0);

    assertThatThrownBy(() -> userService.setActive(USER_ID, false))
            .isInstanceOf(UserNotFoundException.class);
    verify(cardRepository, never()).deactivateByUserId(anyLong());
  }

  private User user() {
    return User.builder()
            .id(USER_ID)
            .email(EMAIL)
            .firstName("Ivan")
            .lastName("Petrov")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
  }
}

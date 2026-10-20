package by.assxmblxr.userservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import by.assxmblxr.userservice.mapper.CardMapperImpl;
import by.assxmblxr.userservice.repository.CardRepository;
import by.assxmblxr.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {
  private static final Long USER_ID = 1L;
  private static final Long CARD_ID = 10L;
  private static final String NUMBER = "4111111111111111";

  @Mock
  private CardRepository cardRepository;

  @Mock
  private UserRepository userRepository;

  @Spy
  private CardMapper cardMapper = new CardMapperImpl();

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache usersCache;

  @InjectMocks
  private CardServiceImpl cardService;

  @Test
  void createAddsCardToUserAndSavesItThroughCascade() {
    User user = user();
    CardCreateRequest request = new CardCreateRequest(NUMBER, "IVAN PETROV", YearMonth.of(2030, 3));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(cardRepository.countActiveByUserId(USER_ID)).thenReturn(4L);
    when(cardRepository.existsByNumber(NUMBER)).thenReturn(false);

    CardResponse response = cardService.create(USER_ID, request);

    assertThat(user.getCards()).hasSize(1);
    Card card = user.getCards().getFirst();
    assertThat(card.getUser()).isSameAs(user);
    assertThat(card.getExpirationDate()).isEqualTo(LocalDate.of(2030, 3, 31));
    assertThat(card.isActive()).isTrue();
    verify(userRepository).lockRowForUpdate(USER_ID);
    verify(userRepository).flush();
    verify(cardRepository, never()).save(any());

    assertThat(response.userId()).isEqualTo(USER_ID);
    assertThat(response.number()).isEqualTo(NUMBER);
    assertThat(response.expirationDate()).isEqualTo(YearMonth.of(2030, 3));
  }

  @Test
  void createThrowsWhenUserNotFound() {
    CardCreateRequest request = new CardCreateRequest(NUMBER, "IVAN PETROV", YearMonth.of(2030, 3));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.create(USER_ID, request))
            .isInstanceOf(UserNotFoundException.class);
    verify(userRepository, never()).lockRowForUpdate(anyLong());
    verify(userRepository, never()).flush();
  }

  @Test
  void createThrowsWhenUserAlreadyHasFiveCards() {
    User user = user();
    CardCreateRequest request = new CardCreateRequest(NUMBER, "IVAN PETROV", YearMonth.of(2030, 3));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(cardRepository.countActiveByUserId(USER_ID)).thenReturn(5L);

    assertThatThrownBy(() -> cardService.create(USER_ID, request))
            .isInstanceOf(CardLimitExceededException.class);
    assertThat(user.getCards()).isEmpty();
    verify(userRepository, never()).flush();
  }

  @Test
  void createThrowsWhenCardNumberAlreadyExists() {
    User user = user();
    CardCreateRequest request = new CardCreateRequest(NUMBER, "IVAN PETROV", YearMonth.of(2030, 3));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(cardRepository.countActiveByUserId(USER_ID)).thenReturn(0L);
    when(cardRepository.existsByNumber(NUMBER)).thenReturn(true);

    assertThatThrownBy(() -> cardService.create(USER_ID, request))
            .isInstanceOf(CardNumberAlreadyExistsException.class);
    assertThat(user.getCards()).isEmpty();
    verify(userRepository, never()).flush();
  }

  @Test
  void findByIdReturnsCard() {
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card(user())));

    CardResponse response = cardService.findById(CARD_ID);

    assertThat(response.id()).isEqualTo(CARD_ID);
    assertThat(response.userId()).isEqualTo(USER_ID);
    assertThat(response.expirationDate()).isEqualTo(YearMonth.of(2030, 3));
  }

  @Test
  void findByIdThrowsWhenCardNotFound() {
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.findById(CARD_ID))
            .isInstanceOf(CardNotFoundException.class);
  }

  @Test
  void findAllReturnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);
    when(cardRepository.findAll(ArgumentMatchers.<Specification<Card>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(card(user())), pageable, 1));

    Page<CardResponse> page = cardService.findAll(null, "petr", pageable);

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent()).extracting(CardResponse::number).containsExactly(NUMBER);
  }

  @Test
  void findByUserIdReturnsUserCards() {
    when(userRepository.existsById(USER_ID)).thenReturn(true);
    when(cardRepository.findByUserId(USER_ID)).thenReturn(List.of(card(user())));

    List<CardResponse> cards = cardService.findByUserId(USER_ID);

    assertThat(cards).extracting(CardResponse::id).containsExactly(CARD_ID);
  }

  @Test
  void findByUserIdThrowsWhenUserNotFound() {
    when(userRepository.existsById(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> cardService.findByUserId(USER_ID))
            .isInstanceOf(UserNotFoundException.class);
    verify(cardRepository, never()).findByUserId(anyLong());
  }

  @Test
  void updateChangesCardFields() {
    Card card = card(user());
    CardUpdateRequest request = new CardUpdateRequest("5555555555554444", "PETR IVANOV", YearMonth.of(2031, 12));
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(cardRepository.existsByNumberAndIdNot("5555555555554444", CARD_ID)).thenReturn(false);

    CardResponse response = cardService.update(CARD_ID, request);

    assertThat(card.getNumber()).isEqualTo("5555555555554444");
    assertThat(card.getHolder()).isEqualTo("PETR IVANOV");
    assertThat(card.getExpirationDate()).isEqualTo(LocalDate.of(2031, 12, 31));
    assertThat(response.number()).isEqualTo("5555555555554444");
  }

  @Test
  void updateThrowsWhenCardNotFound() {
    CardUpdateRequest request = new CardUpdateRequest(NUMBER, "IVAN PETROV", YearMonth.of(2030, 3));
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.update(CARD_ID, request))
            .isInstanceOf(CardNotFoundException.class);
    verify(cardRepository, never()).existsByNumberAndIdNot(anyString(), anyLong());
  }

  @Test
  void updateThrowsWhenNumberTakenByAnotherCard() {
    Card card = card(user());
    CardUpdateRequest request = new CardUpdateRequest("5555555555554444", "PETR IVANOV", YearMonth.of(2031, 12));
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(cardRepository.existsByNumberAndIdNot("5555555555554444", CARD_ID)).thenReturn(true);

    assertThatThrownBy(() -> cardService.update(CARD_ID, request))
            .isInstanceOf(CardNumberAlreadyExistsException.class);
    assertThat(card.getNumber()).isEqualTo(NUMBER);
  }

  @Test
  void setActiveDeactivatesCardWithoutLimitCheck() {
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card(user())));
    when(cacheManager.getCache("users")).thenReturn(usersCache);

    cardService.setActive(CARD_ID, false);

    verify(cardRepository).updateActive(CARD_ID, false);
    verify(usersCache).evict(USER_ID);
    verify(userRepository, never()).lockRowForUpdate(anyLong());
    verify(cardRepository, never()).countActiveByUserId(anyLong());
  }

  @Test
  void setActiveActivatesInactiveCardWhenUnderLimit() {
    Card card = card(user());
    card.setActive(false);
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(cardRepository.countActiveByUserId(USER_ID)).thenReturn(4L);
    when(cacheManager.getCache("users")).thenReturn(usersCache);

    cardService.setActive(CARD_ID, true);

    verify(userRepository).lockRowForUpdate(USER_ID);
    verify(cardRepository).updateActive(CARD_ID, true);
    verify(usersCache).evict(USER_ID);
  }

  @Test
  void setActiveThrowsWhenActivatingCardOverLimit() {
    Card card = card(user());
    card.setActive(false);
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(cardRepository.countActiveByUserId(USER_ID)).thenReturn(5L);

    assertThatThrownBy(() -> cardService.setActive(CARD_ID, true))
            .isInstanceOf(CardLimitExceededException.class);
    verify(userRepository).lockRowForUpdate(USER_ID);
    verify(cardRepository, never()).updateActive(anyLong(), anyBoolean());
    verify(usersCache, never()).evict(any());
  }

  @Test
  void setActiveSkipsLimitCheckWhenCardAlreadyActive() {
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card(user())));
    when(cacheManager.getCache("users")).thenReturn(usersCache);

    cardService.setActive(CARD_ID, true);

    verify(userRepository, never()).lockRowForUpdate(anyLong());
    verify(cardRepository, never()).countActiveByUserId(anyLong());
    verify(cardRepository).updateActive(CARD_ID, true);
  }

  @Test
  void setActiveThrowsWhenCardNotFound() {
    when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.setActive(CARD_ID, true))
            .isInstanceOf(CardNotFoundException.class);
    verify(cardRepository, never()).updateActive(anyLong(), anyBoolean());
    verify(usersCache, never()).evict(any());
  }

  private User user() {
    return User.builder()
            .id(USER_ID)
            .email("ivan@mail.com")
            .firstName("Ivan")
            .lastName("Petrov")
            .build();
  }

  private Card card(User user) {
    return Card.builder()
            .id(CARD_ID)
            .user(user)
            .number(NUMBER)
            .holder("IVAN PETROV")
            .expirationDate(LocalDate.of(2030, 3, 31))
            .build();
  }
}

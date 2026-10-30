package com.innowise.userservice.security;

import com.innowise.userservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component("cardAccess")
@RequiredArgsConstructor
public class CardAccess {
  private final CardRepository cardRepository;

  public boolean isOwner(Long cardId, Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
      return false;
    }
    Long userId = CurrentUser.currentUserId(jwt);
    return cardRepository.findUserIdById(cardId)
            .map(userId::equals)
            .orElse(false);
  }
}

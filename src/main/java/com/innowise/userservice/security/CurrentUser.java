package com.innowise.userservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

public final class CurrentUser {
  private CurrentUser(){}

  public static Long currentUserId(Jwt jwt) {
    if (jwt.getClaim("userId") instanceof Number userId) {
      return userId.longValue();
    }
    throw new InvalidBearerTokenException("Token does not contain userId");
  }
}

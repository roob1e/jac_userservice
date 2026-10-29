package com.innowise.userservice.exception;

public class CardLimitExceededException extends ConflictException {
  public CardLimitExceededException(Long userId, int numberOfCards) {
    super("User " + userId + " already has the maximum of " + numberOfCards + " cards");
  }
}

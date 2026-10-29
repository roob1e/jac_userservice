package com.innowise.userservice.exception;

public class CardNumberAlreadyExistsException extends ConflictException {
  public CardNumberAlreadyExistsException(String cardNumber) {
    super("Card with number " + cardNumber + " already exists");
  }
}

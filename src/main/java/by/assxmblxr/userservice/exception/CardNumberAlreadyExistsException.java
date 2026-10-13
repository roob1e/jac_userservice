package by.assxmblxr.userservice.exception;

public class CardNumberAlreadyExistsException extends RuntimeException {
  public CardNumberAlreadyExistsException(String cardNumber) {
    super("Card with number " + cardNumber + " already exists");
  }
}

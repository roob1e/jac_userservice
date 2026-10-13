package by.assxmblxr.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
  public CardLimitExceededException(Long userId, int numberOfCards) {
    super("User " + userId + " already has the maximum of " + numberOfCards + " cards");
  }
}

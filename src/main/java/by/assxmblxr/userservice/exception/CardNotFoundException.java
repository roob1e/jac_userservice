package by.assxmblxr.userservice.exception;

public class CardNotFoundException extends NotFoundException {
  public CardNotFoundException(Long id) {
    super("Card with id " + id + " not found");
  }
}

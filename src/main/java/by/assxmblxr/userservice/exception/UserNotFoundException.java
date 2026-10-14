package by.assxmblxr.userservice.exception;

public class UserNotFoundException extends NotFoundException {
  public UserNotFoundException(Long id) {
    super("User with id " + id + " not found");
  }
}

package by.assxmblxr.userservice.specification;

import by.assxmblxr.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {
  private UserSpecification() {}

  public static Specification<User> firstNameContains(String firstName) {
    return (root, query, cb) -> firstName == null || firstName.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
  }

  public static Specification<User> lastNameContains(String lastName) {
    return (root, query, cb) -> lastName == null || lastName.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase()+ "%");
  }
}

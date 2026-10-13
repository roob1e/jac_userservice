package by.assxmblxr.userservice.specification;

import by.assxmblxr.userservice.entity.Card;
import by.assxmblxr.userservice.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecification {
  private CardSpecification() {}

  public static Specification<Card> ownersFirstNameContains(String firstName) {
    return (root, query, cb) -> {
      if (firstName == null || firstName.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("firstName")), "%" + firstName.toLowerCase()+ "%");
    };
  }

  public static Specification<Card> ownersLastNameContains(String lastName) {
    return (root, query, cb) -> {
      if (lastName == null || lastName.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("lastName")), "%" + lastName.toLowerCase()+ "%");
    };
  }
}

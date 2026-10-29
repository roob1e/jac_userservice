package com.innowise.userservice.specification;

import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecification {
  private CardSpecification() {}

  public static Specification<Card> ownerFirstNameContains(String firstName) {
    return (root, query, cb) -> {
      if (firstName == null || firstName.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("firstName")), "%" + firstName.toLowerCase()+ "%");
    };
  }

  public static Specification<Card> ownerLastNameContains(String lastName) {
    return (root, query, cb) -> {
      if (lastName == null || lastName.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("lastName")), "%" + lastName.toLowerCase()+ "%");
    };
  }
}

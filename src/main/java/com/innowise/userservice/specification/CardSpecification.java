package com.innowise.userservice.specification;

import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecification {
  private CardSpecification() {}

  public static Specification<Card> ownerNameContains(String name) {
    return (root, query, cb) -> {
      if (name == null || name.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("name")), "%" + name.toLowerCase()+ "%");
    };
  }

  public static Specification<Card> ownerSurnameContains(String surname) {
    return (root, query, cb) -> {
      if (surname == null || surname.isBlank()) {
        return cb.conjunction();
      }
      Join<Card, User> user = root.join("user");
      return cb.like(cb.lower(user.get("surname")), "%" + surname.toLowerCase()+ "%");
    };
  }
}

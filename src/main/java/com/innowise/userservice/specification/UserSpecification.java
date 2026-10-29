package com.innowise.userservice.specification;

import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {
  private UserSpecification() {}

  public static Specification<User> nameContains(String name) {
    return (root, query, cb) -> name == null || name.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
  }

  public static Specification<User> surnameContains(String surname) {
    return (root, query, cb) -> surname == null || surname.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase()+ "%");
  }
}

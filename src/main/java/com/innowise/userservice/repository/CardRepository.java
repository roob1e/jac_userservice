package com.innowise.userservice.repository;

import com.innowise.userservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
  List<Card> findByUserId(Long userId);

  @Modifying(clearAutomatically = true)
  @Query(value = "UPDATE payment_cards SET active = :active WHERE id = :id", nativeQuery = true)
  int updateActive(@Param("id") Long id, @Param("active") boolean active);

  @Modifying(clearAutomatically = true)
  @Query("update Card c set c.active = false where c.user.id = :userId")
  void deactivateByUserId(@Param("userId") Long userId);

  boolean existsByNumber(String number);

  boolean existsByNumberAndIdNot(String number, Long id);

  @Query("select c.user.id from Card c where c.id = :id")
  Optional<Long> findUserIdById(@Param("id") Long id);

  long countByUserIdAndActiveTrue(Long userId);
}

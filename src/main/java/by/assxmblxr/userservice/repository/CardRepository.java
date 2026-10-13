package by.assxmblxr.userservice.repository;

import by.assxmblxr.userservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
  List<Card> findByUserId(Long userId);
  long countByUserId(Long userId);

  @Modifying(clearAutomatically = true)
  @Query(value = "UPDATE cards SET active = :active WHERE id = :id", nativeQuery = true)
  int updateActive(@Param("id") Long id, @Param("active") boolean active);

  @Modifying(clearAutomatically = true)
  @Query("update Card c set c.active = false where c.user.id = :userId")
  int deactivateByUserId(@Param("userId") Long userId);

  boolean existsByNumber(String number);

  boolean existsByNumberAndIdNot(String number, Long id);
}

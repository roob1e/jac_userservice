package by.assxmblxr.userservice.repository;

import by.assxmblxr.userservice.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
  Page<Card> findByUserId(Long userId, Pageable pageable);
  long countByUserId(Long userId);

  @Modifying(clearAutomatically = true)
  @Query(value = "UPDATE cards SET active = :active WHERE id = :id", nativeQuery = true)
  int updateActive(@Param("id") Long id, @Param("active") boolean active);
}

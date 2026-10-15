package by.assxmblxr.userservice.repository;

import by.assxmblxr.userservice.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  boolean existsByEmail(String email);

  @Modifying(clearAutomatically = true)
  @Query("update User u set u.active = :active where u.id = :id")
  int updateActive(@Param("id") Long id, @Param("active") boolean active);

  boolean existsByEmailAndIdNot(String email, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = "cards")
  Optional<User> findWithCardsById(Long id);
}

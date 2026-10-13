package by.assxmblxr.userservice.repository;

import by.assxmblxr.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);

  @Modifying(clearAutomatically = true)
  @Query("update User u set u.active = :active where u.id = :id")
  int updateActive(@Param("id") Long id, @Param("active") boolean active);

  boolean existsByEmailAndIdNot(String email, Long id);
}

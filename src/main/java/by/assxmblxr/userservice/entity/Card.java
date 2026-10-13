package by.assxmblxr.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cards")
public class Card {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column
  private String number;

  @Column
  private String holder;

  @Column
  private LocalDate expirationDate;

  @Column
  @Builder.Default
  private boolean active = true;

  @Column
  @CreationTimestamp
  private Instant createdAt;
}

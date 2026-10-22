package by.assxmblxr.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private String email;

  @Column
  private String firstName;

  @Column
  private String lastName;

  @Column
  @CreationTimestamp
  private Instant createdAt;

  @Column
  @Builder.Default
  private boolean active = true;

  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Card> cards = new ArrayList<>();

  public void addCard(Card card) {
    card.setUser(this);
    cards.add(card);
  }
}

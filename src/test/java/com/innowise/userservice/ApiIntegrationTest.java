package com.innowise.userservice;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.innowise.userservice.repository.CardRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CardRepository cardRepository;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanDatabaseAndCache() {
    jdbcTemplate.execute("TRUNCATE TABLE payment_cards, users RESTART IDENTITY");
    cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
  }

  @Test
  void userCanBeCreatedReadUpdatedAndFound() throws Exception {
    String location = mockMvc.perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();

    mockMvc.perform(get(URI.create(location)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ivan@mail.com"));

    mockMvc.perform(put(URI.create(location))
                    .contentType(APPLICATION_JSON)
                    .content(userJson("petr@mail.com", "Petr", "Ivanov")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Petr"));

    createUser("anna@mail.com", "Anna", "Smirnova");

    mockMvc.perform(get("/api/users").param("name", "pet").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].email").value("petr@mail.com"))
            .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void userCanHaveAtMostFiveCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    for (int i = 1; i <= 5; i++) {
      mockMvc.perform(post("/api/users/{userId}/cards", userId)
                      .contentType(APPLICATION_JSON)
                      .content(cardJson("411111111111111" + i)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.userId").value(userId));
    }

    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111116")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("maximum of 5 cards")));

    mockMvc.perform(get("/api/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].expirationDate").value("03/30"));

    assertThat(cardRepository.findByUserId(userId))
            .hasSize(5)
            .allSatisfy(card -> assertThat(card.getExpirationDate()).isEqualTo(LocalDate.of(2030, 3, 31)));
  }

  @Test
  void onlyActiveCardsCountTowardsTheLimit() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    long firstCardId = 0;
    for (int i = 1; i <= 5; i++) {
      String card = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                      .contentType(APPLICATION_JSON)
                      .content(cardJson("411111111111111" + i)))
              .andExpect(status().isCreated())
              .andReturn().getResponse().getContentAsString();
      if (i == 1) {
        firstCardId = ((Number) JsonPath.read(card, "$.id")).longValue();
      }
    }

    mockMvc.perform(patch("/api/cards/{id}/status", firstCardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111116")))
            .andExpect(status().isCreated());

    mockMvc.perform(patch("/api/cards/{id}/status", firstCardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(true)))
            .andExpect(status().isConflict());
  }

  @Test
  void deactivatingUserDeactivatesTheirCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated());

    mockMvc.perform(patch("/api/users/{id}/status", userId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"active": false}
                            """))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.active").value(false));
    mockMvc.perform(get("/api/users/{userId}/cards", userId))
            .andExpect(jsonPath("$[0].active").value(false));
  }

  @Test
  void cachedUserWithCardsReflectsEveryChange() throws Exception {
    createUser("other@mail.com", "Other", "User");
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(0));

    String card = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(1));

    mockMvc.perform(put("/api/cards/{id}", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].holder").value("NEW HOLDER"));

    mockMvc.perform(patch("/api/cards/{id}/status", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"active": false}
                            """))
            .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].active").value(false));

    mockMvc.perform(put("/api/users/{id}", userId)
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk());
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.surname").value("Sidorov"));
  }

  @Test
  void userByIdIsServedFromCache() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    assertThat(cachedUser(userId)).isNull();

    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.name").value("Ivan"));
    assertThat(cachedUser(userId)).isNotNull();

    jdbcTemplate.update("UPDATE users SET name = 'Changed in DB' WHERE id = ?", userId);

    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.name").value("Ivan"));
  }

  @Test
  void cachedUserIsEvictedByEveryChangingOperation() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    cacheUser(userId);
    mockMvc.perform(put("/api/users/{id}", userId)
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk());
    assertThat(cachedUser(userId)).as("after user update").isNull();

    cacheUser(userId);
    String card = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    assertThat(cachedUser(userId)).as("after card creation").isNull();

    cacheUser(userId);
    mockMvc.perform(put("/api/cards/{id}", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    assertThat(cachedUser(userId)).as("after card update").isNull();

    cacheUser(userId);
    mockMvc.perform(patch("/api/cards/{id}/status", cardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after card status change").isNull();

    cacheUser(userId);
    mockMvc.perform(delete("/api/cards/{id}", cardId))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after card deletion").isNull();

    cacheUser(userId);
    mockMvc.perform(patch("/api/users/{id}/status", userId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after user status change").isNull();

    cacheUser(userId);
    mockMvc.perform(delete("/api/users/{id}", userId))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after user deletion").isNull();
  }

  @Test
  void deletingCardRemovesItAndRefreshesCachedUser() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    String card = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(1));

    mockMvc.perform(delete("/api/cards/{id}", cardId))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/cards/{id}", cardId))
            .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(0));
    assertThat(cardRepository.findByUserId(userId)).isEmpty();
  }

  @Test
  void deletingUserDeletesTheirCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    mockMvc.perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated());
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk());

    mockMvc.perform(delete("/api/users/{id}", userId))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isNotFound());
    assertThat(cardRepository.findByUserId(userId)).isEmpty();
  }

  @Test
  void errorsAreReturnedAsApiErrorWithMatchingStatus() throws Exception {
    createUser("ivan@mail.com", "Ivan", "Petrov");

    mockMvc.perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.path").value("/api/users"));

    mockMvc.perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("not-an-email", "", "Petrov")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("email")))
            .andExpect(jsonPath("$.message").value(containsString("name")));

    mockMvc.perform(get("/api/users/{id}", 999_999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id 999999 not found"));

    mockMvc.perform(get("/api/users/abc"))
            .andExpect(status().isBadRequest());
  }

  private long createUser(String email, String name, String surname) throws Exception {
    String response = mockMvc.perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson(email, name, surname)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private String userJson(String email, String name, String surname) {
    return """
            {"email": "%s", "name": "%s", "surname": "%s", "birthDate": "1990-05-15"}
            """.formatted(email, name, surname);
  }

  private void cacheUser(long userId) throws Exception {
    mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk());
    assertThat(cachedUser(userId)).isNotNull();
  }

  private Cache.ValueWrapper cachedUser(long userId) {
    return requireNonNull(cacheManager.getCache("users")).get(userId);
  }

  private String statusJson(boolean active) {
    return """
            {"active": %s}
            """.formatted(active);
  }

  private String cardJson(String number) {
    return """
            {"number": "%s", "holder": "IVAN PETROV", "expirationDate": "03/30"}
            """.formatted(number);
  }
}

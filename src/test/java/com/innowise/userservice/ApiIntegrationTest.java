package com.innowise.userservice;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
  void requestWithoutTokenIsRejectedWithApiError401() throws Exception {
    mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.path").value("/api/users/me"));
  }

  @Test
  void tokenWithoutUserIdIsRejectedWith401() throws Exception {
    mockMvc.perform(get("/api/users/me")
                    .with(jwt().jwt(token -> token.claim("role", "USER"))
                            .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isUnauthorized());
  }

  @Test
  void userReadsAndUpdatesOnlyOwnProfileThroughMe() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    long otherId = createUser("anna@mail.com", "Anna", "Smirnova");

    mockMvc.perform(get("/api/users/me").with(user(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("ivan@mail.com"));

    mockMvc.perform(put("/api/users/me").with(user(userId))
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.surname").value("Sidorov"));

    mockMvc.perform(get("/api/users/{id}", otherId).with(user(userId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    mockMvc.perform(get("/api/users/{id}", userId).with(user(userId)))
            .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/users").with(user(userId)))
            .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/users/{id}", otherId).with(user(userId)))
            .andExpect(status().isForbidden());
  }

  @Test
  void userManagesOnlyOwnCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    long otherId = createUser("anna@mail.com", "Anna", "Smirnova");
    long foreignCardId = createCard(otherId, "4222222222222222");

    String card = mockMvc.perform(post("/api/users/me/cards").with(user(userId))
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(userId))
            .andReturn().getResponse().getContentAsString();
    long ownCardId = ((Number) JsonPath.read(card, "$.id")).longValue();

    mockMvc.perform(get("/api/users/me/cards").with(user(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ownCardId));

    mockMvc.perform(get("/api/cards/{id}", ownCardId).with(user(userId)))
            .andExpect(status().isOk());
    mockMvc.perform(put("/api/cards/{id}", ownCardId).with(user(userId))
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    mockMvc.perform(patch("/api/cards/{id}/status", ownCardId).with(user(userId))
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/cards/{id}", foreignCardId).with(user(userId)))
            .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/cards/{id}", foreignCardId).with(user(userId)))
            .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/users/{userId}/cards", otherId).with(user(userId))
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4333333333333333")))
            .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/cards").with(user(userId)))
            .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/cards/{id}", ownCardId).with(user(userId)))
            .andExpect(status().isNoContent());
    assertThat(cardRepository.findById(foreignCardId)).isPresent();
  }

  @Test
  void userCanBeCreatedReadUpdatedAndFound() throws Exception {
    String location = perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();

    perform(get(URI.create(location)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ivan@mail.com"));

    perform(put(URI.create(location))
                    .contentType(APPLICATION_JSON)
                    .content(userJson("petr@mail.com", "Petr", "Ivanov")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Petr"));

    createUser("anna@mail.com", "Anna", "Smirnova");

    perform(get("/api/users").param("name", "pet").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].email").value("petr@mail.com"))
            .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void userCanHaveAtMostFiveCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    for (int i = 1; i <= 5; i++) {
      perform(post("/api/users/{userId}/cards", userId)
                      .contentType(APPLICATION_JSON)
                      .content(cardJson("411111111111111" + i)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.userId").value(userId));
    }

    perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111116")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("maximum of 5 cards")));

    perform(get("/api/users/{userId}/cards", userId))
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
      String card = perform(post("/api/users/{userId}/cards", userId)
                      .contentType(APPLICATION_JSON)
                      .content(cardJson("411111111111111" + i)))
              .andExpect(status().isCreated())
              .andReturn().getResponse().getContentAsString();
      if (i == 1) {
        firstCardId = ((Number) JsonPath.read(card, "$.id")).longValue();
      }
    }

    perform(patch("/api/cards/{id}/status", firstCardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());

    perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111116")))
            .andExpect(status().isCreated());

    perform(patch("/api/cards/{id}/status", firstCardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(true)))
            .andExpect(status().isConflict());
  }

  @Test
  void deactivatingUserDeactivatesTheirCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated());

    perform(patch("/api/users/{id}/status", userId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"active": false}
                            """))
            .andExpect(status().isNoContent());

    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.active").value(false));
    perform(get("/api/users/{userId}/cards", userId))
            .andExpect(jsonPath("$[0].active").value(false));
  }

  @Test
  void cachedUserWithCardsReflectsEveryChange() throws Exception {
    createUser("other@mail.com", "Other", "User");
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(0));

    String card = perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(1));

    perform(put("/api/cards/{id}", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].holder").value("NEW HOLDER"));

    perform(patch("/api/cards/{id}/status", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"active": false}
                            """))
            .andExpect(status().isNoContent());
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].active").value(false));

    perform(put("/api/users/{id}", userId)
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk());
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.surname").value("Sidorov"));
  }

  @Test
  void userByIdIsServedFromCache() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    assertThat(cachedUser(userId)).isNull();

    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.name").value("Ivan"));
    assertThat(cachedUser(userId)).isNotNull();

    jdbcTemplate.update("UPDATE users SET name = 'Changed in DB' WHERE id = ?", userId);

    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.name").value("Ivan"));
  }

  @Test
  void cachedUserIsEvictedByEveryChangingOperation() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    cacheUser(userId);
    perform(put("/api/users/{id}", userId)
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk());
    assertThat(cachedUser(userId)).as("after user update").isNull();

    cacheUser(userId);
    String card = perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    assertThat(cachedUser(userId)).as("after card creation").isNull();

    cacheUser(userId);
    perform(put("/api/cards/{id}", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    assertThat(cachedUser(userId)).as("after card update").isNull();

    cacheUser(userId);
    perform(patch("/api/cards/{id}/status", cardId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after card status change").isNull();

    cacheUser(userId);
    perform(delete("/api/cards/{id}", cardId))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after card deletion").isNull();

    cacheUser(userId);
    perform(patch("/api/users/{id}/status", userId)
                    .contentType(APPLICATION_JSON)
                    .content(statusJson(false)))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after user status change").isNull();

    cacheUser(userId);
    perform(delete("/api/users/{id}", userId))
            .andExpect(status().isNoContent());
    assertThat(cachedUser(userId)).as("after user deletion").isNull();
  }

  @Test
  void deletingCardRemovesItAndRefreshesCachedUser() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    String card = perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(1));

    perform(delete("/api/cards/{id}", cardId))
            .andExpect(status().isNoContent());

    perform(get("/api/cards/{id}", cardId))
            .andExpect(status().isNotFound());
    perform(get("/api/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(0));
    assertThat(cardRepository.findByUserId(userId)).isEmpty();
  }

  @Test
  void deletingUserDeletesTheirCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated());
    perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk());

    perform(delete("/api/users/{id}", userId))
            .andExpect(status().isNoContent());

    perform(get("/api/users/{id}", userId))
            .andExpect(status().isNotFound());
    assertThat(cardRepository.findByUserId(userId)).isEmpty();
  }

  @Test
  void errorsAreReturnedAsApiErrorWithMatchingStatus() throws Exception {
    createUser("ivan@mail.com", "Ivan", "Petrov");

    perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.path").value("/api/users"));

    perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("not-an-email", "", "Petrov")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("email")))
            .andExpect(jsonPath("$.message").value(containsString("name")));

    perform(get("/api/users/{id}", 999_999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id 999999 not found"));

    perform(get("/api/users/abc"))
            .andExpect(status().isBadRequest());

    perform(post("/api/users")
                    .contentType(APPLICATION_JSON)
                    .content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));
  }

  @Test
  void cardCanBeFetchedById() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    long cardId = createCard(userId, "4111111111111111");

    perform(get("/api/cards/{id}", cardId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value("4111111111111111"))
            .andExpect(jsonPath("$.userId").value(userId));
  }

  @ParameterizedTest
  @CsvSource({
      "iva,     , 1",
      "   , smir, 1",
      "IVA, PETR, 1",
      "iva, smir, 0",
      "'' , ''  , 2",
      "   ,     , 2"
  })
  void usersAndCardsAreFilteredByOwnerNameAndSurname(String name, String surname, int expected) throws Exception {
    createCard(createUser("ivan@mail.com", "Ivan", "Petrov"), "4111111111111111");
    createCard(createUser("anna@mail.com", "Anna", "Smirnova"), "4222222222222222");

    perform(withNameFilters(get("/api/users"), name, surname))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(expected));
    perform(withNameFilters(get("/api/cards"), name, surname))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(expected));
  }

  private MockHttpServletRequestBuilder withNameFilters(MockHttpServletRequestBuilder request,
                                                        String name, String surname) {
    if (name != null) {
      request.param("name", name);
    }
    if (surname != null) {
      request.param("surname", surname);
    }
    return request;
  }

  private long createCard(long userId, String number) throws Exception {
    String response = perform(post("/api/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson(number)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private long createUser(String email, String name, String surname) throws Exception {
    String response = perform(post("/api/users")
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
    perform(get("/api/users/{id}", userId))
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

  private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request.with(admin()));
  }

  private JwtRequestPostProcessor admin() {
    return jwt().jwt(token -> token.claim("userId", 1_000_000L).claim("role", "ADMIN"))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private JwtRequestPostProcessor user(long userId) {
    return jwt().jwt(token -> token.claim("userId", userId).claim("role", "USER"))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}

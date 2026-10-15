package by.assxmblxr.userservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.assxmblxr.userservice.repository.CardRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    jdbcTemplate.execute("TRUNCATE TABLE cards, users RESTART IDENTITY");
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Test
  void userCanBeCreatedReadUpdatedAndFound() throws Exception {
    String location = mockMvc.perform(post("/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getHeader("Location");

    mockMvc.perform(get(URI.create(location)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ivan@mail.com"));

    mockMvc.perform(put(URI.create(location))
                    .contentType(APPLICATION_JSON)
                    .content(userJson("petr@mail.com", "Petr", "Ivanov")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Petr"));

    createUser("anna@mail.com", "Anna", "Smirnova");

    mockMvc.perform(get("/users").param("firstName", "pet").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].email").value("petr@mail.com"))
            .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void userCanHaveAtMostFiveCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    for (int i = 1; i <= 5; i++) {
      mockMvc.perform(post("/users/{userId}/cards", userId)
                      .contentType(APPLICATION_JSON)
                      .content(cardJson("411111111111111" + i)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.userId").value(userId));
    }

    mockMvc.perform(post("/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111116")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("maximum of 5 cards")));

    mockMvc.perform(get("/users/{userId}/cards", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].expirationDate").value("03/30"));

    assertThat(cardRepository.findByUserId(userId))
            .hasSize(5)
            .allSatisfy(card -> assertThat(card.getExpirationDate()).isEqualTo(LocalDate.of(2030, 3, 31)));
  }

  @Test
  void deactivatingUserDeactivatesTheirCards() throws Exception {
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");
    mockMvc.perform(post("/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated());

    mockMvc.perform(patch("/users/{id}/deactivate", userId))
            .andExpect(status().isNoContent());

    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.active").value(false));
    mockMvc.perform(get("/users/{userId}/cards", userId))
            .andExpect(jsonPath("$[0].active").value(false));
  }

  @Test
  void cachedUserWithCardsReflectsEveryChange() throws Exception {
    createUser("other@mail.com", "Other", "User");
    long userId = createUser("ivan@mail.com", "Ivan", "Petrov");

    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(0));

    String card = mockMvc.perform(post("/users/{userId}/cards", userId)
                    .contentType(APPLICATION_JSON)
                    .content(cardJson("4111111111111111")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    long cardId = ((Number) JsonPath.read(card, "$.id")).longValue();
    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.cards.length()").value(1));

    mockMvc.perform(put("/cards/{id}", cardId)
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"number": "4111111111111111", "holder": "NEW HOLDER", "expirationDate": "03/30"}
                            """))
            .andExpect(status().isOk());
    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].holder").value("NEW HOLDER"));

    mockMvc.perform(patch("/cards/{id}/deactivate", cardId))
            .andExpect(status().isNoContent());
    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.cards[0].active").value(false));

    mockMvc.perform(put("/users/{id}", userId)
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Sidorov")))
            .andExpect(status().isOk());
    mockMvc.perform(get("/users/{id}", userId))
            .andExpect(jsonPath("$.lastName").value("Sidorov"));
  }

  @Test
  void errorsAreReturnedAsApiErrorWithMatchingStatus() throws Exception {
    createUser("ivan@mail.com", "Ivan", "Petrov");

    mockMvc.perform(post("/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("ivan@mail.com", "Ivan", "Petrov")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.path").value("/users"));

    mockMvc.perform(post("/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson("not-an-email", "", "Petrov")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("email")))
            .andExpect(jsonPath("$.message").value(containsString("firstName")));

    mockMvc.perform(get("/users/{id}", 999_999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User with id 999999 not found"));

    mockMvc.perform(get("/users/abc"))
            .andExpect(status().isBadRequest());
  }

  private long createUser(String email, String firstName, String lastName) throws Exception {
    String response = mockMvc.perform(post("/users")
                    .contentType(APPLICATION_JSON)
                    .content(userJson(email, firstName, lastName)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private String userJson(String email, String firstName, String lastName) {
    return """
            {"email": "%s", "firstName": "%s", "lastName": "%s"}
            """.formatted(email, firstName, lastName);
  }

  private String cardJson(String number) {
    return """
            {"number": "%s", "holder": "IVAN PETROV", "expirationDate": "03/30"}
            """.formatted(number);
  }
}

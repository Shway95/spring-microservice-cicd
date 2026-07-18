# 03 - The Spring Boot Application (Every File Explained)

---

## Project Structure

```
spring-microservice-cicd/
├── src/
│   ├── main/
│   │   ├── java/com/shway/microservice/
│   │   │   ├── MicroserviceApplication.java    ← Entry point
│   │   │   ├── config/
│   │   │   │   └── AppConfig.java              ← Reads environment variables
│   │   │   ├── controller/
│   │   │   │   ├── UserController.java         ← CRUD API for users
│   │   │   │   ├── GreetingController.java     ← Hello endpoint
│   │   │   │   ├── EnvController.java          ← Shows current env config
│   │   │   │   └── HealthInfoController.java   ← App info + uptime
│   │   │   └── model/
│   │   │       └── User.java                   ← User data structure
│   │   └── resources/
│   │       └── application.yml                 ← Configuration file
│   └── test/
│       └── java/com/shway/microservice/
│           ├── MicroserviceApplicationTests.java  ← Context test
│           └── controller/
│               └── UserControllerTest.java        ← API tests
├── build.gradle                                ← Build configuration
├── settings.gradle                             ← Project name
├── Dockerfile                                  ← Container instructions
├── sonar-project.properties                    ← SonarCloud config
├── .gitignore                                  ← Files Git should ignore
└── .github/workflows/
    ├── ci.yml                                  ← CI pipeline
    └── cd.yml                                  ← CD pipeline
```

---

## build.gradle (Line by Line)

```groovy
plugins {
    id 'java'                                                // This is a Java project
    id 'org.springframework.boot' version '3.5.3'            // Spring Boot framework
    id 'io.spring.dependency-management' version '1.1.7'     // Auto-manages dependency versions
    id 'jacoco'                                              // Code coverage reports
}

group = 'com.shway'                    // Package group (like a namespace)
version = '0.0.1-SNAPSHOT'            // Project version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // Use Java 21
    }
}

repositories {
    mavenCentral()    // Download dependencies from Maven Central (like npm registry)
}

dependencies {
    // spring-boot-starter-web: gives us REST API, JSON handling, embedded Tomcat
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // spring-boot-starter-actuator: gives us /actuator/health endpoint
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Testing dependencies (only used during tests, not in production)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()           // Use JUnit 5 to run tests
    finalizedBy jacocoTestReport // After tests, generate coverage report
}

jacocoTestReport {
    dependsOn test               // Coverage report depends on tests running first
    reports {
        xml.required = true      // Generate XML report (SonarCloud needs this)
    }
}
```

---

## application.yml (Configuration)

```yaml
spring:
  application:
    name: spring-microservice-cicd           # App name (shows in logs)
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}    # Which profile to use
    # ${VAR:default} means: read VAR from environment, if not set use "default"
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:microservice_dev}
    username: ${DB_USERNAME:admin}
    password: ${DB_PASSWORD:password}

server:
  port: ${SERVER_PORT:8080}    # App runs on port 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,env   # Expose these actuator endpoints
  endpoint:
    health:
      show-details: always         # Show full health details

app:
  api-base-url: ${API_BASE_URL:http://localhost:8080}
  api-key: ${API_KEY:default-key}
  log-level: ${LOG_LEVEL:DEBUG}
```

**How `${VAR:default}` works:**
- Running locally (no env vars set) → uses `localhost`, `admin`, `DEBUG`
- Running in Kubernetes (ConfigMap sets env vars) → uses `dev-db.internal`, `dev_user`, etc.
- **Zero code changes** between environments!

---

## MicroserviceApplication.java (Entry Point)

```java
package com.shway.microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // This annotation does everything:
                        // - Enables auto-configuration
                        // - Enables component scanning (finds @RestController, etc.)
                        // - Marks this as the starting class
public class MicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceApplication.class, args);
        // This starts the embedded Tomcat server on port 8080
    }
}
```

---

## UserController.java (REST API)

```java
@RestController                    // This class handles HTTP requests
@RequestMapping("/api/users")      // All endpoints start with /api/users
public class UserController {

    private final List<User> users = new ArrayList<>();  // In-memory database
    private final AtomicLong counter = new AtomicLong(); // Auto-increment ID

    @GetMapping                    // GET /api/users → returns all users
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")           // GET /api/users/1 → returns user with id=1
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());  // 404 if not found
    }

    @PostMapping                   // POST /api/users + JSON body → creates user
    public ResponseEntity<User> createUser(@RequestBody User user) {
        user.setId(counter.incrementAndGet());  // Assign auto ID
        users.add(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);  // Return 201
    }

    @DeleteMapping("/{id}")        // DELETE /api/users/1 → deletes user with id=1
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean removed = users.removeIf(u -> u.getId().equals(id));
        return removed ? ResponseEntity.noContent().build()    // 204 success
                       : ResponseEntity.notFound().build();    // 404 not found
    }
}
```

---

## AppConfig.java (Environment Variables)

```java
@Configuration    // Spring creates this bean at startup
public class AppConfig {

    @Value("${app.api-base-url}")      // Reads from application.yml → env var
    private String apiBaseUrl;

    @Value("${app.api-key}")
    private String apiKey;

    @Value("${app.log-level}")
    private String logLevel;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // Getters so other classes can access these values
    public String getApiBaseUrl() { return apiBaseUrl; }
    public String getApiKey() { return apiKey; }
    public String getLogLevel() { return logLevel; }
    public String getActiveProfile() { return activeProfile; }
}
```

**How this connects to Kubernetes:**
1. ConfigMap sets `API_BASE_URL=https://dev-api.example.com`
2. K8s injects it as an environment variable into the pod
3. `application.yml` reads `${API_BASE_URL:http://localhost:8080}`
4. Spring sets `app.api-base-url = https://dev-api.example.com`
5. `AppConfig` class reads it via `@Value`
6. Any controller can inject `AppConfig` and call `getApiBaseUrl()`

---

## API Endpoints Summary

| Method | URL | What it does | Example |
|--------|-----|-------------|---------|
| GET | `/api/users` | List all users | `curl http://localhost:8080/api/users` |
| GET | `/api/users/1` | Get user by ID | `curl http://localhost:8080/api/users/1` |
| POST | `/api/users` | Create a user | `curl -X POST -H "Content-Type: application/json" -d '{"name":"John","email":"j@x.com"}' http://localhost:8080/api/users` |
| DELETE | `/api/users/1` | Delete a user | `curl -X DELETE http://localhost:8080/api/users/1` |
| GET | `/api/greet` | Hello message | `curl http://localhost:8080/api/greet` |
| GET | `/api/greet/John` | Greet by name | `curl http://localhost:8080/api/greet/John` |
| GET | `/api/env` | Current environment | `curl http://localhost:8080/api/env` |
| GET | `/api/info` | App info + uptime | `curl http://localhost:8080/api/info` |
| GET | `/actuator/health` | Health status | `curl http://localhost:8080/actuator/health` |

---

## Tests Explained

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// ↑ Starts the full app on a random port for testing
class UserControllerTest {

    @LocalServerPort
    private int port;        // The random port the test app started on

    @Autowired
    private TestRestTemplate restTemplate;   // HTTP client for testing

    @Test
    void shouldReturnEmptyListInitially() {
        // Call GET /api/users and verify it returns 200 OK with empty list
        ResponseEntity<User[]> response = restTemplate.getForEntity(getBaseUrl(), User[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldCreateUser() {
        // Call POST /api/users with a user and verify it returns 201 CREATED
        User user = new User(null, "John Doe", "john@example.com");
        ResponseEntity<User> response = restTemplate.postForEntity(getBaseUrl(), user, User.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());  // ID should be auto-assigned
    }

    @Test
    void shouldReturn404ForNonExistentUser() {
        // Call GET /api/users/999 and verify it returns 404
        ResponseEntity<User> response = restTemplate.getForEntity(getBaseUrl() + "/999", User.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

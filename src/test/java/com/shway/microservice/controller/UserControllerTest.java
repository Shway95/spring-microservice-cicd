package com.shway.microservice.controller;

import com.shway.microservice.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/users";
    }

    @Test
    void shouldReturnEmptyListInitially() {
        ResponseEntity<User[]> response = restTemplate.getForEntity(getBaseUrl(), User[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateUser() {
        User user = new User(null, "John Doe", "john@example.com");
        ResponseEntity<User> response = restTemplate.postForEntity(getBaseUrl(), user, User.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("John Doe", response.getBody().getName());
    }

    @Test
    void shouldReturn404ForNonExistentUser() {
        ResponseEntity<User> response = restTemplate.getForEntity(getBaseUrl() + "/999", User.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

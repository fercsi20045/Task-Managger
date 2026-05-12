package com.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.model.Task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // application-test.properties használata
public class TaskManagerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    /**
     * Minden teszt előtt regisztrálunk és bejelentkezünk egy tesztfelhasználóval.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Regisztráció
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("tesztuser");
        reg.setEmail("teszt@example.com");
        reg.setPassword("jelszo123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Bejelentkezés és token kiolvasása
        LoginRequest login = new LoginRequest();
        login.setUsername("tesztuser");
        login.setPassword("jelszo123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(body).get("token").asText();
    }

    /**
     * 1. TESZT: Regisztráció ismételt felhasználónévvel 400-at ad vissza
     */
    @Test
    void register_duplicateUsername_returnsBadRequest() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("tesztuser"); // már létezik
        reg.setEmail("masik@example.com");
        reg.setPassword("jelszo123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 2. TESZT: Feladat létrehozása és listázása
     */
    @Test
    void createTask_thenList_returnsTask() throws Exception {
        // Feladat létrehozása
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Tesztelési feladat");
        taskRequest.setDescription("Ez egy teszt leírás");

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tesztelési feladat"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn();

        // Listázás: a feladatnak szerepelnie kell
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Tesztelési feladat')]").exists());
    }

    /**
     * 3. TESZT: Státusz módosítása
     */
    @Test
    void updateStatus_taskBecomesInProgress() throws Exception {
        // Feladat létrehozása
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setTitle("Státusz teszt feladat");

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Státusz módosítása IN_PROGRESS-re
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    /**
     * 4. TESZT: Token nélkül nem érhető el a /api/tasks végpont
     */
    @Test
    void getTasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }
}

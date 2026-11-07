package com.ganzithon.backend.malangi.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MalangiApiTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void newMemberSeesDefaultSlangiAndLockedCatalog() throws Exception {
        String accessToken = registerAndLogin("malangi-list@example.com");

        mockMvc.perform(get("/api/v1/malangis").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.malangis.length()").value(3))
                .andExpect(jsonPath("$.data.malangis[0].malangiType").value("SLANGI"))
                .andExpect(jsonPath("$.data.malangis[0].name").value("슬랑이"))
                .andExpect(jsonPath("$.data.malangis[0].isUnlocked").value(true))
                .andExpect(jsonPath("$.data.malangis[0].isSelected").value(true))
                .andExpect(jsonPath("$.data.malangis[1].malangiType").value("SQUISHY"))
                .andExpect(jsonPath("$.data.malangis[1].isUnlocked").value(false))
                .andExpect(jsonPath("$.data.malangis[2].malangiType").value("WAKPPU"))
                .andExpect(jsonPath("$.data.malangis[2].totalPlayTime").value(0));
    }

    @Test
    void selectsUnlockedMalangiAndMakesItTheOnlySelectedItem() throws Exception {
        String email = "malangi-select@example.com";
        String accessToken = registerAndLogin(email);
        assertThat(unlock(email, "SQUISHY")).isEqualTo(1);

        mockMvc.perform(patch("/api/v1/malangis/SQUISHY/select")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedMalangiType").value("SQUISHY"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/malangis").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.malangis[0].isSelected").value(false))
                .andExpect(jsonPath("$.data.malangis[1].isSelected").value(true));
    }

    @Test
    void rejectsLockedAndUnknownMalangiSelection() throws Exception {
        String accessToken = registerAndLogin("malangi-locked@example.com");

        mockMvc.perform(patch("/api/v1/malangis/WAKPPU/select")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LOCKED"));

        mockMvc.perform(get("/api/v1/malangis").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.malangis[0].isSelected").value(true))
                .andExpect(jsonPath("$.data.malangis[2].isSelected").value(false));

        mockMvc.perform(patch("/api/v1/malangis/UNKNOWN/select")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TYPE"));
    }

    @Test
    void keepsInventoryAndSelectionIsolatedBetweenUsers() throws Exception {
        String firstEmail = "malangi-first@example.com";
        String firstAccessToken = registerAndLogin(firstEmail);
        String secondAccessToken = registerAndLogin("malangi-second@example.com");
        assertThat(unlock(firstEmail, "SQUISHY")).isEqualTo(1);

        mockMvc.perform(patch("/api/v1/malangis/SQUISHY/select")
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/malangis").header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.malangis[0].isSelected").value(true))
                .andExpect(jsonPath("$.data.malangis[1].isUnlocked").value(false))
                .andExpect(jsonPath("$.data.malangis[1].isSelected").value(false));
    }

    @Test
    void requiresAccessTokenForInventoryAndSelection() throws Exception {
        mockMvc.perform(get("/api/v1/malangis")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/malangis/SLANGI/select"))
                .andExpect(status().isUnauthorized());
    }

    private int unlock(String email, String malangiType) {
        return jdbcTemplate.update("INSERT INTO user_malangis (user_id, malangi_type, is_unlocked, is_selected) "
                + "VALUES ((SELECT id FROM users WHERE email = ?), ?, TRUE, FALSE)", email, malangiType);
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!","nickname":"말랑친구"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    }
}

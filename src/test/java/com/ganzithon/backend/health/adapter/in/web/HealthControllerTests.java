package com.ganzithon.backend.health.adapter.in.web;

import com.ganzithon.backend.config.HealthConfig;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(HealthConfig.class)
class HealthControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    void returnsUpWithoutCredentialsOrDatabase() throws Exception {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }

    @Test
    void doesNotExposeUnversionedHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPost() throws Exception {
        mockMvc.perform(post("/api/v1/health"))
                .andExpect(status().isMethodNotAllowed());
    }
}

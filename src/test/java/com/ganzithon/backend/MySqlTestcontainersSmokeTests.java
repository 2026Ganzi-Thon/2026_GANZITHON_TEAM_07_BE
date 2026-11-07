package com.ganzithon.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MySqlTestcontainersSmokeTests {

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Flyway flyway;

    @Test
    void connectsToMySqlAndRunsAuthenticationMigration() {
        assertThat(jdbcTemplate.queryForObject("SELECT VERSION()", String.class)).startsWith("8.4");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getVersion().getVersion())
                .containsExactly("1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_name IN ('users', 'refresh_tokens', 'user_malangis')", Integer.class))
                .isEqualTo(3);
    }
}

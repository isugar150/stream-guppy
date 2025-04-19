package com.namejm.stream_guppy.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
    "stream_guppy.database=h2",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabaseConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseConfig databaseConfig;

    @Test
    void contextLoads() {
        assertNotNull(dataSource);
        assertNotNull(databaseConfig);
        System.out.println("[DEBUG_LOG] Database configuration loaded successfully");
    }
}
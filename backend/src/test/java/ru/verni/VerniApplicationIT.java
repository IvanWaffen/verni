package ru.verni;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(properties = "spring.mail.host=localhost")
@Import(TestcontainersConfiguration.class)
class VerniApplicationIT {

  @Autowired JdbcClient jdbc;

  @Test
  void contextLoadsAndPostgisIsAvailable() {
    String version = jdbc.sql("SELECT postgis_version()").query(String.class).single();
    assertThat(version).isNotBlank();
  }
}

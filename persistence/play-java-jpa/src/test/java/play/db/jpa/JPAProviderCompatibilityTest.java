/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.util.Properties;
import org.hibernate.Version;
import org.junit.Test;

public class JPAProviderCompatibilityTest {

  @Test
  public void shouldUseExpectedProviderAndApiVersions() throws Exception {
    String hibernateVersion = System.getProperty("play.jpa.expectedHibernateVersion");
    String persistenceVersion = System.getProperty("play.jpa.expectedPersistenceVersion");
    assertThat(hibernateVersion).isNotBlank();
    assertThat(persistenceVersion).isNotBlank();
    assertThat(Version.getVersionString()).isEqualTo(hibernateVersion);

    // The API's manifest omits milestone suffixes; Maven metadata contains the exact version.
    Properties apiMetadata = new Properties();
    try (InputStream metadata =
        EntityManager.class.getResourceAsStream(
            "/META-INF/maven/jakarta.persistence/jakarta.persistence-api/pom.properties")) {
      assertThat(metadata).isNotNull();
      apiMetadata.load(metadata);
    }
    assertThat(apiMetadata.getProperty("version")).isEqualTo(persistenceVersion);
  }
}

package ru.verni;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifies module boundaries and the AR-03 dependency graph. */
class ModularityTest {

  @Test
  void verifiesModuleStructure() {
    ApplicationModules.of(VerniApplication.class).verify();
  }
}

/** Demo sandboxes; active only in the demo deployment (ADR-0001). */
@ApplicationModule(
    allowedDependencies = {"shared", "identity", "listings", "responses", "messaging"})
@NullMarked
package ru.verni.demo;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;

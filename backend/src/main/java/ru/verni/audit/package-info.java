/** History of significant actions. May reference other modules' event types only. */
@ApplicationModule(
    allowedDependencies = {
      "shared",
      "identity",
      "listings",
      "responses",
      "messaging",
      "moderation",
      "media"
    })
@NullMarked
package ru.verni.audit;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;

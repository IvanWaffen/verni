/** Email notifications via the outbox. May reference other modules' event types only. */
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
package ru.verni.notifications;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;

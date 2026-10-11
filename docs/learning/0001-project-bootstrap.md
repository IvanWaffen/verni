# 0001. Project bootstrap

- **Commit:** `chore: bootstrap project skeleton and documentation` (before PR workflow existed)
- **Topics:** maven, spring-boot, configuration, profiles, modulith, dependency-injection, security, flyway, jpa, testcontainers, nullability

## What was done

The backend skeleton was generated with Spring Initializr and adapted to our rules: one package per module with enforced dependencies, deny-by-default security, Flyway migrations, and two kinds of tests (fast unit tests and Docker-based integration tests).

## New concepts

### Maven and `pom.xml`

**In Laravel:** `composer.json` + `composer.lock`; `require` vs `require-dev`; `composer install`.
**In Java/Spring:** `pom.xml` declares dependencies and the **build**. Composer only installs packages, but Maven also compiles, runs tests and packages a `.jar`. Some details:

- **Parent POM** `spring-boot-starter-parent` (`backend/pom.xml:7`) pins versions of hundreds of libraries that are tested together. That is why most dependencies have no `<version>`. The closest analogue is `laravel/framework` constraining its `illuminate/*` packages.
- **Starters** (`spring-boot-starter-webmvc`, `-data-jpa`, ...) are meta-packages: one dependency pulls in a consistent set.
- **Scopes:** `test` ≈ `require-dev`; `runtime` = needed to run but not to compile (the PostgreSQL JDBC driver).
- **`mvnw`** is the Maven Wrapper: it downloads the right Maven version, so nobody has to install Maven. Always use `./mvnw`, not `mvn`.
- **Lifecycle:** `./mvnw verify` runs the phases in order: `compile → test → package → verify`. There is no lock file: versions are pinned exactly in the POM / parent.

### Two test runners: Surefire and Failsafe

**In Laravel:** `phpunit` with `Unit` and `Feature` suites.
**In Java/Spring:** classes named `*Test` run with **Surefire** in the `test` phase: fast, no Docker. Classes named `*IT` run with **Failsafe** in the `integration-test`/`verify` phases: they start a real PostgreSQL in Docker. `./mvnw test` runs only the fast ones.
**In our code:** `backend/src/test/java/ru/verni/ModularityTest.java` (unit), `backend/src/test/java/ru/verni/VerniApplicationIT.java` (integration).

### Application entry point and auto-configuration

**In Laravel:** `public/index.php` → `bootstrap/app.php`; package auto-discovery registers service providers.
**In Java/Spring:** `VerniApplication.main()` starts an embedded Tomcat. There is no PHP-FPM/nginx in front: the JVM process *is* the server and lives for a long time. `@SpringBootApplication` turns on:

- **component scanning:** every `@Component`/`@Service`/`@Configuration` in `ru.verni.*` becomes a bean;
- **auto-configuration:** a library on the classpath configures itself (add the JPA starter → you get a `DataSource` and an `EntityManager`), similar to package discovery.

**In our code:** `backend/src/main/java/ru/verni/VerniApplication.java`

### Dependency injection: `@Configuration` + `@Bean`

**In Laravel:** a `ServiceProvider::register()` with `$this->app->singleton(...)`; constructor injection resolved by the container.
**In Java/Spring:** a `@Configuration` class whose `@Bean` methods return objects the container manages (singletons by default). Constructor injection works the same way as in Laravel, without annotations when there is one constructor.
**In our code:** `backend/src/main/java/ru/verni/shared/security/SecurityConfiguration.java:11-15`

Annotations such as `@Bean` are like PHP 8 attributes (`#[Route]`), but Spring reads them at startup to build the container.

### Spring Security: deny by default

**In Laravel:** middleware groups (`auth`, `guest`) attached to routes; `VerifyCsrfToken` with the `XSRF-TOKEN` cookie.
**In Java/Spring:** one `SecurityFilterChain` bean describes which URLs are public (`permitAll`) and requires authentication for everything else (`anyRequest().authenticated()`). CSRF works like Laravel's: `CookieCsrfTokenRepository.withHttpOnlyFalse()` puts the token in a cookie readable by JavaScript, and the frontend sends it back in a header. This is the same idea as the `XSRF-TOKEN` cookie used by Axios.
**In our code:** `SecurityConfiguration.java:16-26`

### Configuration and profiles

**In Laravel:** `.env` + `config/*.php`, `APP_ENV=local`.
**In Java/Spring:** `application.yaml`. A YAML document after `---` with `spring.config.activate.on-profile: local` applies only when the `local` profile is active (`-Dspring-boot.run.profiles=local`). Every property can be overridden by an environment variable with relaxed binding: `spring.datasource.url` ← `SPRING_DATASOURCE_URL`. On servers we configure through environment variables, not files.
**In our code:** `backend/src/main/resources/application.yaml:48`

### Packages, visibility and module boundaries

**In Laravel:** namespaces; everything `public` is reachable from everywhere. For boundaries you would need a tool like Deptrac.
**In Java/Spring:**

- A **package** is a namespace and a directory. A class **without** `public` is *package-private*: other packages cannot see it. PHP has no such thing. `SecurityConfiguration` is package-private on purpose.
- `package-info.java` holds annotations for the whole package.
- **Spring Modulith** plays the role Deptrac would play: each module's `package-info.java` declares `allowedDependencies`, and `ModularityTest` fails the build if a module uses a module it may not use, or touches another module's internal subpackages.

**In our code:** `backend/src/main/java/ru/verni/responses/package-info.java:2`

### Null safety with JSpecify

**In Laravel/PHP:** types are non-nullable unless written `?string`.
**In Java:** any object reference can be `null` by default, and the compiler does not complain. `@NullMarked` in `package-info.java` flips the default for the package: types are non-null unless annotated `@Nullable`. IDEs and Spring use it for warnings. This is a convention checked by tooling, not a runtime guarantee like PHP types.

### Flyway migrations

**In Laravel:** `php artisan migrate`, PHP classes with `up()`/`down()`.
**In Java/Spring:** plain SQL files in `db/migration`, named `V<version>__<description>.sql`. They are applied automatically **on application startup**, in version order, and recorded in `flyway_schema_history`. There is **no `down()`**: to undo, write a new migration. An applied file must never be edited, because Flyway checks checksums and refuses to start.
**In our code:** `backend/src/main/resources/db/migration/`

### JPA settings that prevent classic mistakes

**In Laravel:** Eloquent never changes the schema by itself; lazy loading in Blade causes N+1 (`Model::preventLazyLoading()`).
**In Java/Spring:**

- `ddl-auto: validate`: Hibernate *can* generate the schema from entities. We forbid that and only check that entities match the Flyway schema (`application.yaml:10`).
- `open-in-view: false`: by default Spring keeps the DB session open while rendering the response, so lazy loading "just works" in the web layer, the same N+1 trap as in Blade. We turn it off (`application.yaml:8`).

### Testcontainers

**In Laravel:** `RefreshDatabase` against a test MySQL/SQLite database you must run yourself.
**In Java/Spring:** the test starts a throwaway `postgis/postgis` Docker container. `@ServiceConnection` wires its URL and credentials into Spring automatically. We use real PostgreSQL rather than an in-memory substitute, so PostGIS functions and locks behave as in production.
**In our code:** `backend/src/test/java/ru/verni/TestcontainersConfiguration.java`

## Pitfalls coming from PHP

- **Long-lived process.** Beans are singletons shared by all requests and threads. Never keep per-request state in a field of a `@Service`; in PHP every request starts from scratch.
- **Compilation.** Errors show up in `./mvnw compile`, before anything runs. Restart (or devtools) is needed after changes. There is no "edit and refresh".
- **`==` vs `equals()`.** For objects (including `String`), `==` compares references. Use `.equals()`.
- **Virtual threads** (`spring.threads.virtual.enabled: true`) let each request run on a cheap thread. Blocking JDBC calls are fine, which is close to PHP's "one request = one worker" mental model.

## Read more

- Spring Boot reference, "Externalized Configuration": https://docs.spring.io/spring-boot/reference/features/external-config.html
- Spring Modulith reference, "Fundamentals": https://docs.spring.io/spring-modulith/reference/fundamentals.html
- Flyway, "Migrations" concept: https://documentation.red-gate.com/fd/migrations-271585107.html

## Exercise (optional)

1. In `ru.verni.listings`, create a class that imports something from `ru.verni.responses`. Run `./mvnw test` and read how `ModularityTest` explains the violation. Then delete the class.
2. Run the app with the `local` profile, then override the port with `SERVER_PORT=8081 ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`. This shows relaxed binding at work.

# Laravel → Spring glossary

A cumulative map of concepts. Each row links to the learning note where the concept appeared in our code.

| Laravel / PHP | Java / Spring | Note |
|---|---|---|
| `composer.json` / `composer install` | `pom.xml` / `./mvnw` (Maven) | [0001](0001-project-bootstrap.md) |
| `require-dev` | `<scope>test</scope>` | [0001](0001-project-bootstrap.md) |
| `laravel/framework` pinning `illuminate/*` | `spring-boot-starter-parent` dependency management | [0001](0001-project-bootstrap.md) |
| PHPUnit `Unit` / `Feature` suites | Surefire `*Test` / Failsafe `*IT` | [0001](0001-project-bootstrap.md) |
| `public/index.php` + PHP-FPM | `main()` + embedded Tomcat, long-lived JVM | [0001](0001-project-bootstrap.md) |
| Package auto-discovery | Auto-configuration | [0001](0001-project-bootstrap.md) |
| `ServiceProvider::register()` | `@Configuration` + `@Bean` | [0001](0001-project-bootstrap.md) |
| Service container | Spring `ApplicationContext` (beans) | [0001](0001-project-bootstrap.md) |
| PHP 8 attributes `#[...]` | Annotations `@...` | [0001](0001-project-bootstrap.md) |
| `auth` middleware, route groups | `SecurityFilterChain` | [0001](0001-project-bootstrap.md) |
| `VerifyCsrfToken` + `XSRF-TOKEN` cookie | `CookieCsrfTokenRepository` | [0001](0001-project-bootstrap.md) |
| `.env` + `config/*.php` | `application.yaml` + environment variables | [0001](0001-project-bootstrap.md) |
| `APP_ENV` | Spring profiles | [0001](0001-project-bootstrap.md) |
| Namespaces | Packages (+ package-private visibility) | [0001](0001-project-bootstrap.md) |
| Deptrac | Spring Modulith `verify()` | [0001](0001-project-bootstrap.md) |
| `?string` nullable types | JSpecify `@NullMarked` / `@Nullable` | [0001](0001-project-bootstrap.md) |
| `php artisan migrate` | Flyway (SQL files, run on startup, no `down()`) | [0001](0001-project-bootstrap.md) |
| `Model::preventLazyLoading()` | `spring.jpa.open-in-view=false` | [0001](0001-project-bootstrap.md) |
| `RefreshDatabase` with a test DB | Testcontainers + `@ServiceConnection` | [0001](0001-project-bootstrap.md) |

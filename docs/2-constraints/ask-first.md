# Ask First

⚠️ Stop and confirm with the user before taking any of these actions.
Do not proceed on your own judgement - risk of silent, hard-to-reverse mistakes
is too high.

## Dependencies

- ⚠️ Add any new dependency (package, library, or plugin)
- ⚠️ Upgrade an existing dependency version

## Architecture & structure

- ⚠️ Introduce a new architectural layer or abstraction not currently in the project
- ⚠️ Rename a public API method, class, or module (breaking change)
- ⚠️ Move a file or module to a different directory or package
- ⚠️ Add a new submodule or service to a multi-module project

## Tests

- ⚠️ Modify an existing test's assertions or structure
  (fixing a broken test is OK - changing what it verifies requires confirmation)
- ⚠️ Add a new test profile or test infrastructure configuration

## CI / deployment

- ⚠️ Modify `.github/workflows/` or any CI pipeline file
- ⚠️ Change `Dockerfile` or `docker-compose.yml`
- ⚠️ Modify any production or staging environment configuration

## Java - Configuration

- ⚠️ Modify `application.yml`, `application.properties`, or any Spring profile config
- ⚠️ Modify any `@Configuration` class
- ⚠️ Add or change a Spring Boot auto-configuration exclusion

## Java - Database

- ⚠️ Create, modify, or delete a database migration file
- ⚠️ Change an `@Entity` field type, name, or constraint
- ⚠️ Add or remove a database index

## Redis

- ⚠️ Change an existing cache key pattern (invalidates all cached data)
- ⚠️ Remove or shorten a TTL on an existing cache entry
- ⚠️ Add a new cacheable entity (confirm eviction strategy first)

## Java - Security

- ⚠️ Modify the `SecurityFilterChain` configuration
- ⚠️ Change any `@PreAuthorize` expression on an existing endpoint
- ⚠️ Add or remove a security filter
- ⚠️ Change the authentication mechanism (e.g. JWT -> session)

## Java - Logging

- ⚠️ Add asynchronous log appenders or custom logging filters
- ⚠️ Change default log backup locations or retention boundaries

## Java - Code

- ⚠️ Introduce new thread pool setups, execution thread boundaries, or core JVM parameter configs
- ⚠️ Introduce new MapStruct configuration defaults or complex multi-source mapper interfaces
- ⚠️ Establish new package segments or modify the global project layering structure
- ⚠️ Introduce custom Lombok compilation extensions or less common Lombok annotations

## Spring Boot

- ⚠️ Introduce new global filters, interceptors, or complex AOP aspects
- ⚠️ Create custom Spring Boot starter modules or advanced configuration beans

## Java - Testing

- ⚠️ Change target test coverage requirements or lower pipeline check rules
- ⚠️ Introduce heavy testing frameworks or custom test container configurations

## Java - Database

- ⚠️ Add or remove a database index
- ⚠️ Create temporary tables or define complex, nested database views

## Java - Security

- ⚠️ Introduce new cryptographic algorithms, password encoders, or custom security filters
- ⚠️ Establish new public or unauthenticated endpoints in Web Security config files

## From dev-standards repo

- ⚠️ ASK FIRST before altering CI/CD pipeline definitions or Dockerfiles
- ⚠️ ASK FIRST before modifying production-specific configuration files or cloud environment settings
- ⚠️ ASK FIRST before changing core system directories or base architectural naming rules
- ⚠️ ASK FIRST before introducing a new architectural pattern, utility framework, or cross-cutting library
- ⚠️ ASK FIRST before upgrading the language compiler version
- ⚠️ ASK FIRST before establishing new thread pool setups or altering execution thread boundaries
- ⚠️ ASK FIRST before changing core JVM parameter configs
- ⚠️ ASK FIRST before introducing custom Lombok compilation extensions or uncommon Lombok annotations
- ⚠️ ASK FIRST before changing target test coverage requirements or lowering pipeline check rules
- ⚠️ ASK FIRST before introducing heavy testing frameworks or custom test container configurations
- ⚠️ ASK FIRST before establishing new asynchronous log appenders or custom logging filters
- ⚠️ ASK FIRST before changing default log backup locations or retention boundaries
- ⚠️ ASK FIRST before introducing new indexes or modifying index patterns
- ⚠️ ASK FIRST before creating temporary tables or defining complex nested database views
- ⚠️ ASK FIRST before introducing new cryptographic algorithms, password encoders, or custom security filters
- ⚠️ ASK FIRST before establishing new public or unauthenticated endpoints in Web Security config files
- ⚠️ ASK FIRST before introducing new global filters, interceptors, or complex AOP aspects
- ⚠️ ASK FIRST before creating custom Spring Boot starter modules or advanced configuration beans

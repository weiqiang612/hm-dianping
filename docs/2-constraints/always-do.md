# Always Do

✅ These behaviours are mandatory in every session, for every task.
Not optional even for "quick" or "small" changes.

## Session start

- ✅ Check if the dev server is running - if not, run `bash init.sh` before writing code
- ✅ The SessionStart hook injects git state and CURRENT_PLAN.md automatically
- ✅ Read the active feature's `spec.md` in `docs/3-tasks/features/` before implementing
- ✅ Check `docs/2-constraints/never-do.md` before any non-trivial change

## Before committing

- ✅ Run `mvn test` - all tests must pass
- ✅ Confirm no secrets or credentials are staged: `git diff --cached`
- ✅ Update `docs/3-tasks/features/<active-task>/tasks.md` - check off completed tasks
- ✅ Update `docs/3-tasks/CURRENT_PLAN.md` - mark feature complete when all tasks done

## General

- ✅ Use explicit types - never use implicit or `any` typing
- ✅ Define constants for any value used more than once
- ✅ Write tests before marking a task done
- ✅ Write tests in `given / when / then` or `arrange / act / assert` structure
- ✅ Run `mvn test` after every logical change - fix failures before continuing

## Java - Code style

- ✅ Declare all method parameters `final`
- ✅ Declare local variables `final` wherever possible
- ✅ Use `@Override` on every method that overrides or implements an interface
- ✅ Use wrapper types (`Integer`, `Long`, etc.) for POJO fields and RPC parameters
- ✅ Use `"constant".equals(variable)` - never `variable.equals("constant")`
- ✅ Check for null/empty before operating on collections or strings

## Java - Logging

- ✅ Use parameterised log messages: `log.info("msg: {}", value)` - no string concatenation
- ✅ Include contextual identifiers (`userId`, `requestId`) in all log messages

## Java - API

- ✅ Return the standard response envelope for all REST endpoints
- ✅ Map all exceptions through `@ControllerAdvice` - never let stack traces reach clients
- ✅ Use `@Validated` + Bean Validation on all Controller method parameters

## Java - Naming

- ✅ `UpperCamelCase` for class names, `lowerCamelCase` for methods and variables
- ✅ `UPPER_SNAKE_CASE` for constants
- ✅ Suffix exceptions with `Exception`, test classes with `Test`, implementations with `Impl`
- ✅ Service/DAO method prefixes: `get` (single), `list` (collection), `count`,
  `save`/`insert`, `remove`/`delete`, `update`

## Redis

- ✅ Respect cache TTLs and key conventions when reading or writing Redis data

## Java - Persistence

- ✅ Use explicit `resultMap` definitions in MyBatis XML mappers
- ✅ Use `lowercase_snake_case` for table and field names
- ✅ Include `id`, `gmt_create`, and `gmt_modified` in every table

## Java - Testing

- ✅ Write all tests using JUnit 5 and Mockito for dependency isolation
- ✅ Follow AIR (Automatic, Independent, Repeatable) for all test suites
- ✅ Structure test cases with clear `given / when / then` segments

## Java - Lombok

- ✅ Use `@RequiredArgsConstructor` with `final` fields for Spring DI
- ✅ Use granular annotations like `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` on DTO/VO models

## Java - Database

- ✅ Use explicit columns in SQL and keep joins to a small, indexed set
- ✅ Verify destructive operations with a prior SELECT before updating or deleting

## Java - Security

- ✅ Mask sensitive information before presenting or transmitting it
- ✅ Validate all user-supplied input on the server side
- ✅ Apply CSRF protection where forms or AJAX POST submissions exist

## Spring Boot

- ✅ Use constructor-based dependency injection, preferably with `@RequiredArgsConstructor`
- ✅ Implement global exception handling with `@RestControllerAdvice` and `@ExceptionHandler`
- ✅ Validate Controller inputs with `@Validated` and strict bean validation annotations
- ✅ Keep transactional scopes narrow and plan explicit rollback strategies
- ✅ Map distinct business exceptions to their appropriate HTTP status codes

## From dev-standards repo

- ✅ Run local test suites, formatting checks, and linter tasks before committing code or pushing branches
- ✅ Inspect the staged `git diff` to verify that no credentials, debug comments, or temporary hacks are checked in
- ✅ Pull the latest state of the main branch before checking out a new feature branch
- ✅ Write descriptive, standardized Git commit messages
- ✅ Write a single, clear technical summary in the Pull Request
- ✅ Keep methods, components, and classes focused on a single responsibility
- ✅ Clean up temporary debug outputs before checking in code
- ✅ Destructure objects when accessing and using properties
- ✅ Declare all method parameters as `final`
- ✅ Declare local variables as `final` wherever possible
- ✅ Override `toString()` in all POJO and data transfer classes
- ✅ Use wrapper types for POJO class attributes and RPC signatures
- ✅ Use `Objects.equals()` for null-safe comparison
- ✅ Ensure `hashCode()` is overridden whenever `equals()` is overridden
- ✅ Specify an explicit initial capacity for `HashMap` and `ArrayList` instances when size is known
- ✅ Traverse maps using `entrySet()` or `Map.forEach()`
- ✅ Lock `CountDownLatch` updates inside a `finally` block
- ✅ Use `ThreadLocalRandom` for generating random elements across multi-threaded runs
- ✅ Pre-compile regex `Pattern` definitions as `static final` constants
- ✅ Use `System.currentTimeMillis()` instead of `new Date().getTime()`
- ✅ Use 4 spaces for indentation and limit line length to 120 characters
- ✅ Use `@Slf4j` for loggers and include tracing identifiers in logs
- ✅ Retain server log files for a minimum of 15 days
- ✅ Use `JUnit 5`, `Mockito`, AIR, BCDE, and `given / when / then` in tests
- ✅ Reach minimum 70% statement coverage globally

# Never Do

🚫 These are absolute prohibitions. No exceptions, regardless of how convenient
it might seem. If you believe a rule should be broken, stop and ask the user first.

## Security & secrets

- 🚫 Commit secrets, credentials, API keys, or tokens to the repository
- 🚫 Log sensitive user data (passwords, tokens, full card numbers, PII)
- 🚫 Output un-sanitised or un-escaped user data to HTML or API responses
- 🚫 Skip server-side input validation on any public API endpoint

## File boundaries

- 🚫 Edit `node_modules/`, `vendor/`, or any auto-generated directory
- 🚫 Modify `.github/workflows/` or CI pipeline files without explicit approval
- 🚫 Modify production environment configuration files
- 🚫 Modify files outside the scope defined in the active Spec

## Tests

- 🚫 Delete or comment out a failing test to make a build pass
- 🚫 Modify a test's assertions to match wrong behaviour - fix the implementation
- 🚫 Use `console.log` / `System.out` in tests - use assertions

## Java - Code

- 🚫 Use `var` - always use explicit types
- 🚫 Use `@Data` on JPA entities or any class where `equals`/`hashCode` matters
- 🚫 Use magic numbers or inline string literals - define named constants
- 🚫 Catch an exception and silently discard it (empty catch block)
- 🚫 Return from a `finally` block
- 🚫 Throw or declare checked exceptions from Service layer outward
- 🚫 Use `new Thread(...)` directly - always use a thread pool
- 🚫 Create thread pools with `Executors` factory methods (OOM risk)
- 🚫 Use `SimpleDateFormat` as a shared static variable without synchronisation
- 🚫 Use deprecated classes or methods
- 🚫 Use pinyin, mixed pinyin-English, or Chinese characters in identifiers
- 🚫 Use leading/trailing underscores or dollar signs in element names
- 🚫 Set default values inside POJO classes
- 🚫 Prefix POJO boolean fields with `is`
- 🚫 Compare wrapper types with `==`
- 🚫 Use `Arrays.asList()` for lists that need mutation

## Java - Architecture

- 🚫 `Controller` imports or calls `Repository` directly
- 🚫 `Controller` contains business logic beyond routing and input validation
- 🚫 Any layer other than `Service` uses `@Transactional` at class level
- 🚫 Circular Spring bean dependencies - never use `@Lazy` or `@Order` to resolve them
- 🚫 Use field injection (`@Autowired` on private fields) in production code
- 🚫 Perform slow external calls inside an active `@Transactional` scope

## Java - Database & SQL

- 🚫 `SELECT *` - always list columns explicitly
- 🚫 String-concatenated SQL - always use parameterised queries
- 🚫 `${}` in MyBatis XML - use `#{}` only
- 🚫 `FLOAT` or `DOUBLE` for monetary or precision decimal values - use `DECIMAL`
- 🚫 Database-level foreign key constraints or cascades - manage at application layer
- 🚫 Stored procedures
- 🚫 DELETE or destructive UPDATE without a prior SELECT to verify the target set

## Java - API

- 🚫 Expose exception stack traces in API responses
- 🚫 Return raw `HashMap` or `Hashtable` as response body - use typed VO classes
- 🚫 Modify an existing versioned endpoint in a breaking way - create a new version

## Redis

- 🚫 Cache without an explicit TTL - no indefinite caching
- 🚫 Invent cache key patterns - follow the project's established convention
- 🚫 Place cache logic in `Service` layer - it belongs in `Manager` or a dedicated composable/hook

## Java - Logging

- 🚫 Declare loggers manually in source code - use `@Slf4j`
- 🚫 Build log statements via string concatenation
- 🚫 Write raw system outputs or manual stack traces in production code
- 🚫 Log sensitive personal user data in production logs

## Java - Security

- 🚫 Build dynamic SQL statements using string concatenation
- 🚫 Bypass input validation on any public Controller, Open API, or RPC endpoint
- 🚫 Output un-sanitised or un-escaped user inputs directly to HTML views or script executions

## Spring Boot

- 🚫 Apply `@Transactional` to Controller, DAO, or Repository layers
- 🚫 Mask structural design loops with lazy initialization or bean ordering tricks

## From dev-standards repo

- 🚫 NEVER force-push directly to protected branches
- 🚫 NEVER commit large binary assets over 5MB directly into the Git repository
- 🚫 NEVER leave commented-out production code blocks inside standard source files
- 🚫 NEVER bypass pre-commit hooks or local lint validations using `--no-verify`
- 🚫 NEVER hardcode environment-specific configurations; externalize them as environment variables
- 🚫 NEVER use manual loops to copy or clone arrays and objects; use spread syntax or `Object.assign()`
- 🚫 NEVER pass mutable objects as default parameters in function signatures
- 🚫 NEVER use `Executors` factory methods for thread pools
- 🚫 NEVER use `new Thread(...)` directly
- 🚫 NEVER use `SimpleDateFormat` as a static shared instance without synchronization
- 🚫 NEVER use `@Data` on JPA entities or other stable equality-bearing objects
- 🚫 NEVER declare SLF4J loggers manually; use `@Slf4j`
- 🚫 NEVER use string concatenation for SQL or log statements
- 🚫 NEVER use `${}` in MyBatis XML
- 🚫 NEVER write or invoke stored procedures
- 🚫 NEVER use `FLOAT` or `DOUBLE` for money or high precision values
- 🚫 NEVER create database-level foreign keys or cascades
- 🚫 NEVER perform dynamic SQL by string concatenation
- 🚫 NEVER bypass server-side validation
- 🚫 NEVER output unmasked PII in logs or API payloads
- 🚫 NEVER use field injection in production code
- 🚫 NEVER perform slow operations inside active transactional database locks

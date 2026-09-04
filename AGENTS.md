# AGENTS.md

## Project
- **Name**: hm-dianping
- **Stack**: Java 8, Spring Boot 2.3.12.RELEASE, Maven, MyBatis-Plus, Redis, MySQL, Lombok
- **Rule**: This file is an index only. All details live in `docs/`.

## Session start
The `.claude/settings.json` SessionStart hook runs automatically on every session -
it injects git state and current plan context without any action needed.

Run `bash init.sh` only when the dev server is not running (first time setup,
or after a machine restart). Do not run it every session.

## Commands
- **Build**: `mvn clean compile`
- **Test**: `mvn test`
- **Lint**: `# none configured`

## Boundaries (read before acting)
| Before you...                         | Read this first                         |
|---------------------------------------|-----------------------------------------|
| Write or modify any code              | `docs/2-constraints/never-do.md`        |
| Take any action you are unsure about  | `docs/2-constraints/ask-first.md`       |
| Start a session                       | `docs/2-constraints/always-do.md`       |
| Touch any code area                   | `docs/1-standards/README.md`            |
| Work on a specific task               | `docs/3-tasks/features/<TASK-NNN>/spec.md` |
| Start a new session                   | `docs/3-tasks/CURRENT_PLAN.md`          |
| Make an architectural decision        | `docs/2-constraints/adr/`               |

## Workflow
1. If the dev server is not running -> `bash init.sh`. Otherwise skip.
2. Read `docs/3-tasks/CURRENT_PLAN.md` - orient to current stage and active feature.
3. Read `docs/3-tasks/features/<active-task>/spec.md` - do not start without a confirmed spec.
4. Check `docs/2-constraints/never-do.md` before every non-trivial change.
5. Run `mvn test` early and often. Fix failures before continuing.
6. Commit working increments - do not accumulate large uncommitted diffs.
7. Update task progress in `docs/3-tasks/features/<active-task>/tasks.md` as you go.
8. Update `docs/3-tasks/CURRENT_PLAN.md` when the feature is complete.

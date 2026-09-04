# Features

This directory contains one subdirectory per feature, created by the `/new-task` skill.

## Structure

Each feature directory follows this layout:

```
TASK-{NNN}-{slug}/
├── spec.md     # What to build + acceptance criteria (human-confirmed, agent reads)
└── tasks.md    # Executable checklist + key decisions (agent maintains)
```

## Creating a new task

Run `/new-task` with a natural language description:

```
/new-task User can log in with phone number and password
```

The skill will:
1. Draft a spec.md with acceptance criteria
2. Ask clarifying questions for any ambiguities
3. Confirm the implementation approach with you
4. Generate tasks.md with an ordered, executable checklist
5. Update CURRENT_PLAN.md to point to the new feature

## Active task

See `../CURRENT_PLAN.md` for the currently active feature.

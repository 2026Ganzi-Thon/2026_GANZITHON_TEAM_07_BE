# Repository instructions

## Start every task

1. Read `docs/agents/workflow.md` before changing files. Follow its worktree,
   branch, commit, and GitHub PR conventions.
2. Inspect `git status --short --branch`, `git remote -v`, and
   `git worktree list`. Preserve existing changes and identify the task's branch.
3. Before planning or implementing features, fixing behavior, or changing project
   setup, read `docs/agents/development.md`. Follow its document routing, scope,
   specification, and completion rules. Read the domain docs below when exploring
   domain concepts.
4. Run checks appropriate to the change. Report what changed, the checks actually
   run, and any remaining limits.

## Pull requests

Always create a task branch, push it to the personal fork (`origin`), and open a
PR targeting the team repository. Never push `main` or push directly to the team
repository. Never bypass the pre-push hook with `--no-verify` or disabled hooks.

Before writing or updating a PR, read `docs/agents/pr.md` and use
`.github/pull_request_template.md` for the body.

## Agent skills

### Issue tracker

Track issues in `2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE`.
Read `docs/agents/issue-tracker.md` before issue operations.

### Triage labels

Use the five canonical triage labels.
Read `docs/agents/triage-labels.md` before triaging.

### Domain docs

Use a single-context layout.
Read `docs/agents/domain.md` before exploring domain concepts.

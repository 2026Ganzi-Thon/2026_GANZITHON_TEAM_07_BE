# Repository instructions

## Start every task

1. Read `docs/agents/workflow.md` before changing files. Follow its branch,
   commit, optional worktree, and GitHub PR conventions.
2. Inspect `git status --short --branch`, `git remote -v`, and
   `git worktree list`. Preserve existing changes and identify the task's branch.
3. Before planning or implementing features, fixing behavior, or changing project
   setup, read `docs/agents/development.md`. Follow its document routing, scope,
   specification, and completion rules. Read the domain docs below when exploring
   domain concepts.
4. Run checks appropriate to the change. Report what changed, the checks actually
   run, and any remaining limits.

## Pull requests

Always create a task branch, push it to the shared team repository (`origin`),
and open a PR targeting its `main`. Keep local `main` aligned with `origin/main`;
unmerged work stays on task branches. Never push directly to `main`. Never bypass the pre-push hook with `--no-verify` or disabled hooks.

Before writing or updating a PR, read `docs/agents/pr.md` and use
`.github/pull_request_template.md` for the body.

## Agent skills

### Issue tracker

Track issues as Git-tracked Markdown in `.scratch/<feature>/`.
Read `docs/agents/issue-tracker.md` before issue operations.

### Triage labels

Use the five canonical triage states in local issue files.
Read `docs/agents/triage-labels.md` before triaging.

### Domain docs

Use a single-context layout.
Read `docs/agents/domain.md` before exploring domain concepts.

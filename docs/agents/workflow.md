# Engineering workflow

## Start work

Use one task branch per independent task in the main repository folder.
Continue the current task branch when it matches the request. Preserve unrelated
and uncommitted changes; stage only files belonging to the task.

Before editing, inspect status, remotes, worktrees, and the relevant ticket.
Read `docs/agents/issue-tracker.md` for Git-tracked Markdown tickets in `.scratch/`.
Use `origin` for the shared team repository:
`https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git` (or its SSH URL).
Each contributor needs team repository write access. Create work in a task branch,
push that branch to `origin`, and open a PR into the team's `main`.

Keep local `main` aligned with `origin/main`. Unmerged work belongs on task
branches. To start a new task after preserving any existing changes:

```sh
git fetch origin
git switch main
git pull --ff-only origin main
git switch --no-track -c feat/login origin/main
```

If the fast-forward fails, preserve the local work in a named branch and inspect
the divergence before changing `main`. Keep local-only commits on that branch.
Do not reset away work or combine unmerged tasks into local `main`.

Branch format: `<type>/<issue-number>-<short-kebab-description>`.
For Markdown tickets use `<type>/<short-kebab-description>`, such as
`feat/login`; reference `.scratch/` paths rather than treating local numbers as
GitHub issue numbers. Use the commit types below as branch prefixes.

The first `git push -u origin <branch>` sets the branch's tracking reference to
`origin/<branch>`. Before VS Code Sync, verify that reference. Local `main` tracks
`origin/main` and is updated by pull, not push. See `docs/vscode.md` for editor use.

### Optional worktrees

Use a worktree only when parallel work needs an isolated checkout. By default,
edit source in the main repository folder and keep shared work records in
`.scratch/`. For an optional parallel checkout:

```sh
mkdir -p .worktrees
git worktree add --no-track -b feat/parallel-task .worktrees/feat-parallel-task origin/main
```

Worktrees share committed history but have separate files and indexes. A branch
can be checked out in only one worktree. Open that folder or preserve its work
and release the branch before moving it. Install dependencies separately, use
different server ports, and avoid recursive searches through `.worktrees/`.
Remove an unused, clean worktree with `git worktree remove`; preserve all local
files first. Delete merged branches with `git branch -d` only when Git confirms
they are merged; retain branches when squash merging prevents that check.

## Commit conventions

Use `<type>(<scope>): <summary>`, with optional scope. Use Korean prose and retain
code identifiers in their original form. Keep the subject concise, use an active
description, and omit a trailing period.

| Type | Purpose |
| --- | --- |
| feat | New behavior |
| fix | Bug fix |
| refactor | Internal change preserving behavior |
| docs | Documentation |
| test | Test additions or corrections |
| chore | Repository maintenance and tooling |
| build | Build system or dependency changes |
| ci | CI workflow changes |
| perf | Performance improvement |
| revert | Reverting a change |

Examples: `feat(auth): 로그인 API 추가`,
`fix(member): 탈퇴한 회원의 로그인 차단`,
`chore: AI 협업 워크플로 설정`.

Keep one logical change per commit. Inspect the staged diff and run relevant
checks before committing. Write the reason and tradeoffs in the body when they
are not clear from the diff. Reference team issues with `Refs #123` when useful.
For breaking changes, use `!` after the type/scope and include a
`BREAKING CHANGE:` footer describing the compatibility impact and migration.

The root `.gitmessage` is the commit template. Configure it once per clone from
the primary checkout (linked worktrees share this local Git configuration):

```sh
git config --local commit.template "$PWD/.gitmessage"
```

The template assists interactive commits; agents using `git commit -m` must
still follow this convention. This is a team convention, not an installed hook.

## GitHub pull requests

Read `docs/agents/pr.md` and use `.github/pull_request_template.md` before writing
or updating a PR. Target `2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE`, base `main`.
After committing and validating the task:

```sh
git push -u origin feat/login
gh pr create --repo 2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE \
  --base main --head feat/login \
  --title 'feat(auth): 로그인 API 추가' --body-file /tmp/task-pr-body.md
```

Replace the example branch and write the exact body to a file first. Use a draft
for incomplete work. Review the diff and CI checks before merging according to
the team's process. Report the PR URL and validation results. After the team PR
is merged, return to `main` and run `git pull --ff-only origin main`. Keep an
unfinished branch available until the work is merged and local files are safe.

## Local push guard

Enable the tracked hook once per clone:

```sh
chmod +x .githooks/pre-push
git config --local core.hooksPath "$PWD/.githooks"
git config --local remote.pushDefault origin
git config --local push.default simple
```

The hook permits task branch updates through the team `origin` only. It blocks
local or remote `main`, tags, deletions, other remotes, unrelated repositories,
and multiple push URLs. It supports the team's HTTPS and SSH URL forms.
Validate it with `python3 scripts/test_pre_push.py` before changing its behavior.
Use the hook normally; never bypass it with `--no-verify` or disabled hooks.

The hook is local and must be installed in each clone. Server-side protection
requires a repository administrator to protect `main`, require PR review and
passing `CI / build-and-test` checks, and block force pushes and deletions.
The checked-in workflow adds that CI check; it does not change GitHub protection
settings by itself.

## Validation

Run checks appropriate to the modified files. CI runs the push guard tests,
Java 21 build and tests, and MySQL 8.4 integration tests. For documentation-only
changes, inspect links and run `git diff --check`. Keep secrets and environment
files out of commits and work records.

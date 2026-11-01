# Engineering workflow

## Start work

Use one task branch per independent task in the main repository folder.
Continue the current task branch when it matches the request. Preserve unrelated
and uncommitted changes; stage only files belonging to the task.

Before editing, inspect status, remotes, worktrees, and the relevant local ticket.
Read `docs/agents/issue-tracker.md` for the Git-tracked `.scratch/` ticket layout.
This clone uses `origin` for the personal fork and `upstream` for the team repo.
Verify URLs in each new clone. Publishing follows this route:
task branch → personal fork (`origin`) → PR to the team repository (`upstream`).
Never push `main`, including the fork's `main`, or push any branch directly to
the team repository. If `origin` points to the team repo, configure a personal
fork as `origin` and the team repo as `upstream` before publishing.

For a new task, first preserve existing changes, then run:

```sh
git fetch upstream
git -c branch.autoSetupMerge=false switch -c feat/123-login upstream/main
```

Fetch and branch creation must succeed before editing. If the team default
branch changes, use that branch instead of `main`. Configure the fork/team
remotes as above if an `upstream` remote is missing.

Branch format: `<type>/<issue-number>-<short-kebab-description>`.
Without a GitHub issue, use `<type>/<short-kebab-description>`, for example
`build/backend-bootstrap`. Local ticket references use `.scratch/` paths;
do not use their local numbers as GitHub issue references.
Use the commit types below for branch prefixes too.

The command disables automatic tracking of `upstream/main`. The first
`git push -u origin <branch>` sets tracking to the personal fork. Before using
VS Code Sync, verify tracking is `origin/<branch>`. Read `docs/vscode.md` when
choosing the working folder or troubleshooting synchronization.

### Optional worktrees

Use a worktree only when parallel work needs an isolated checkout. Otherwise,
keep source edits in the main repository folder; `.scratch/` holds local work
records shared through Git and PRs, not another source checkout.

```sh
mkdir -p .worktrees
git -c branch.autoSetupMerge=false worktree add -b feat/parallel-task .worktrees/feat-parallel-task upstream/main
```

Worktrees share committed history and have separate files and indexes. A branch
already checked out in one worktree cannot be checked out in another. Open its
folder instead, or move the work after preserving changes and freeing the branch.
Install dependencies separately and use distinct ports for parallel servers.
Avoid recursive file searches through `.worktrees/` from the primary checkout.

After a worktree is no longer needed and its changes are committed, remove it
using `git worktree remove .worktrees/<directory>`. Never force removal to
discard work. Use `git branch -d <branch>` only when Git confirms it is merged;
retain it if squash merging prevents that check.

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

Track work locally as described in `docs/agents/issue-tracker.md`.
Target `2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE` and its `main` branch, or its
current default branch if changed.
Read `docs/agents/pr.md` for PR writing rules and use
`.github/pull_request_template.md` as the source of truth for the body structure.

For this fork-based clone, after committing the task and when publishing the
branch/PR is part of the user's request:

```sh
git push -u origin feat/123-login
gh pr create --repo 2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE \
  --base main --head WooDDANG:feat/123-login \
  --title 'feat(auth): 로그인 API 추가' --body-file /tmp/task-pr-body.md
```

Replace example values and write the actual PR body to the file first. In
another contributor's clone, use their fork owner instead of `WooDDANG`. In a
new clone, set up that contributor's personal fork as `origin` first.
Pass `--draft` if the published work is still incomplete. Report the PR URL and
verification results once created. Merge according to the team's review process.

## Local push guard

Enable the checked-in pre-push hook once per clone, from the primary checkout:

```sh
chmod +x .githooks/pre-push
git config --local core.hooksPath "$PWD/.githooks"
```

The absolute path lets linked worktrees use the same hook. Keep the primary
checkout and hook file in place. The hook allows only task branch pushes to
the configured personal fork `origin`; it blocks pushes from `main`, updates
to remote `main`, tag/deletion pushes, and pushes to the team repo or another
remote. It accepts GitHub HTTPS and SSH URLs for a personal repo with the same
repository name, and rejects the team owner. Verify that `origin` is an actual
fork when setting up a new clone.

Never bypass this guard with `--no-verify` or by changing hook configuration.
Local hooks do not configure GitHub branch protection and can be bypassed by
other clients; team-wide enforcement requires GitHub repository rules.

## Validation

Discover build and test commands from the checked-in project configuration.
For documentation-only changes, inspect the diff, links, and `git diff --check`.
When no build or test configuration exists, state that rather than inventing a
passing test result. Never include credentials or local environment files in a
commit or PR.

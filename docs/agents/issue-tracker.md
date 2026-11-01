# Issue tracker: Local Markdown

Issues and specs for this repo live as Git-tracked Markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues are one file per ticket at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01`, never a single combined tickets file
- Triage state is recorded as a `Status:` line near the top of each issue file (see `triage-labels.md` for the role strings)
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `.scratch/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket.

- **Map**: `.scratch/<effort>/map.md` (the Notes / Decisions-so-far / Fog body).
- **Child ticket**: `.scratch/<effort>/issues/NN-<slug>.md`, numbered from `01`, with the question in the body. A `Type:` line records the ticket type (`research`/`prototype`/`grilling`/`task`); a `Status:` line records `claimed`/`resolved`.
- **Blocking**: a `Blocked by: NN, NN` line near the top. A ticket is unblocked when every file it lists is `resolved`.
- **Frontier**: scan `.scratch/<effort>/issues/` for files that are open, unblocked, and unclaimed; first by number wins.
- **Claim**: set `Status: claimed` and save before any work.
- **Resolve**: append the answer under an `## Answer` heading, set `Status: resolved`, then append a context pointer (gist + link) to the map's Decisions-so-far in `map.md`.

## Local files and GitHub PRs

Track plans, specs, tickets, and work history in `.scratch/` through commits and
PRs so the team can follow changes. Start at `.scratch/README.md`. Keep secrets,
raw environment files, and personal settings out of these records. The existing
`preserved-before-bootstrap/` directory is an ignored personal backup.
Maintain accepted terminology in `GLOSSARY.md` and durable decisions in
`docs/adr/` and product docs. Publishing a ticket means creating its Markdown
file and sharing it through the normal Git/PR flow, not creating a GitHub issue.

Code still follows task branch → personal fork (`origin`) → PR to
`2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE`. Follow `workflow.md` and `pr.md`.
Local ticket numbers are not GitHub issue numbers; in a PR's Related Issues
section, use `없음` when there is no GitHub issue and describe any local work
context directly in the PR.

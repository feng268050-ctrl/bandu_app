---
name: push-to-gitlab
description: Initializes or reuses a Git repo in the current project directory, ensures a sensible .gitignore, commits changes, configures GitLab remote origin when safe, and pushes with upstream tracking. Use when the user asks to upload or push the current project to GitLab, paste a GitLab repo URL, or says to publish the workspace to a new GitLab remote.
---

# Push project to GitLab

When the user provides a GitLab repository URL (placeholder: `<GITLAB_REPO_URL>`), runs this workflow **from the project root** they intend to publish (confirm root if ambiguous: `package.json`, `.git`, or user-stated path).

**Never** without explicit user approval: change `origin` URL, `git push --force`, history rewrite (`reset --hard`, `rebase` onto unrelated remote to overwrite, etc.), or `git config --global`. **Never** print or invent real tokens, passwords, or private keys—only tell the user how to configure credentials.

If `origin` already exists, **stop and ask** whether to replace it or use a different remote name; do not overwrite automatically.

---

## Required output format (every step)

For each step the agent executes or describes, use this structure:

```text
Step X: <步骤名称>
Purpose: <本步骤目的>
Command:
<具体命令>
Result:
<执行结果或预期结果>
```

On failure, append:

```text
Error Analysis:
<错误原因>
Fix Suggestion:
<修复方案>
```

Do not skip steps. Do not omit commands the user or agent should run. If a step cannot proceed (e.g. user must decide on `origin`), still emit the step block and state that execution is **paused pending user input**.

---

## Step sequence

### Step 1: Confirm project root and detect Git

**Purpose:** Ensure operations run in the correct directory and know if `.git` exists.

**Command:**

```bash
pwd
# or equivalent; on Windows PowerShell: Get-Location
test -d .git && echo "GIT_REPO" || echo "NOT_GIT_REPO"
# or: git rev-parse --is-inside-work-tree
```

**Result:** State whether the directory is already a Git repository.

**If not a repo:** run `git init` in that root, then report Result.

**Command:**

```bash
git init
```

---

### Step 2: `.gitignore`

**Purpose:** Avoid committing dependencies, secrets, build output, and editor junk.

**Command:**

```bash
test -f .gitignore && echo "HAS_GITIGNORE" || echo "NO_GITIGNORE"
```

**If `.gitignore` is missing**, infer project type and create one:

| Signals | Type |
|---------|------|
| `package.json` or `pnpm-lock.yaml` / `yarn.lock` | **Node** |
| `pyproject.toml`, `requirements.txt`, `setup.py` | **Python** |
| Neither / unclear | **Generic** |

**Node template** (merge with any existing; create file if missing):

```gitignore
node_modules/
.env
.env.*
!.env.example
dist/
build/
.next/
out/
*.log
.idea/
.vscode/
.DS_Store
```

**Python template:**

```gitignore
__pycache__/
*.py[cod]
*$py.class
.Python
venv/
.venv/
.env
dist/
build/
*.egg-info/
*.log
.idea/
.vscode/
```

**Generic template:**

```gitignore
.env
dist/
build/
*.log
.idea/
.vscode/
.DS_Store
```

**Result:** Note whether file was created, updated, or already present.

---

### Step 3: Stage and commit

**Purpose:** Record current tree before push.

**Command:**

```bash
git add .
git status
```

**If there is nothing to commit** (clean working tree, no new files): state clearly that there are **no changes to commit** and skip `git commit` for this round.

**If there are changes:**

**Command:**

```bash
git commit -m "init: first commit"
```

**Result:** Show commit hash or “nothing to commit”. If the first commit message is inappropriate because the repo already has history, use a short descriptive message instead **without** rewriting past commits—and say why the message differed.

**Note:** If commit fails because `user.name` / `user.email` are unset, **do not** set `--global`. Tell the user to run `git config user.name` / `user.email` locally in the repo, or pause and ask them for values to set **only** with `git config` (local, no `--global`).

---

### Step 4: Current branch

**Purpose:** Push the correct ref.

**Command:**

```bash
git branch --show-current
```

**Result:** Branch name (`main`, `master`, or other). Use this as `<branch>` below.

---

### Step 5: Configure remote `origin`

**Purpose:** Point `origin` at `<GITLAB_REPO_URL>` without clobbering an existing remote.

**Command:**

```bash
git remote -v
```

**If `origin` exists:** print current URLs for fetch/push, **pause**, and ask the user whether to:

- keep `origin` and add a different remote name (e.g. `gitlab`) with `<GITLAB_REPO_URL>`, or
- replace `origin` (only after **explicit** user confirmation).

**Do not** run `git remote set-url` or `git remote remove` until the user clearly chooses.

**If `origin` does not exist:**

**Command:**

```bash
git remote add origin <GITLAB_REPO_URL>
```

Normalize URL only if the user agrees (HTTPS vs SSH). Accept both `https://host/group/repo.git` and `git@host:group/repo.git` forms as provided.

**Result:** Confirm remote configuration after user decision or successful `add`.

---

### Step 6: Push and set upstream

**Purpose:** Upload branch and set `upstream`.

**Command:**

```bash
git push -u origin <branch>
```

**Result:** Success output, or errors (see below).

---

## Push failure triage (append Error Analysis + Fix Suggestion)

| Symptom | Likely cause | Fix suggestion (no secrets) |
|--------|----------------|------------------------------|
| `authentication failed` (HTTPS) | Bad password / need token | Use GitLab **Personal Access Token** as password, or credential manager; check username. |
| `Permission denied (publickey)` | SSH key missing / not on GitLab | Add SSH public key in GitLab → SSH Keys; test `ssh -T git@<host>`. |
| `failed to push some refs` / `non-fast-forward` | Remote has commits you lack | `git fetch origin`, then `git pull origin <branch> --rebase` **or** merge with `--allow-unrelated-histories` if histories are unrelated—**only** after explaining; never `--force` without user approval. |
| `src refspec main does not match any` | Wrong branch name or no commits | Use `git branch` / create commit first. |
| `remote origin already exists` | Adding duplicate remote | Use `git remote set-url` only after user confirms replacement, or pick another remote name. |

---

## Checklist before finishing

- [ ] User saw each step in the required **Step / Purpose / Command / Result** format.
- [ ] No unauthorized `origin` overwrite, `--force` push, or global config changes.
- [ ] No real tokens or keys were generated or echoed.
- [ ] If push succeeded: local `<branch>` tracks `origin/<branch>` (`git status -sb` shows upstream).

---

## Optional reference

For GitLab URL patterns only (documentation, not live credentials): HTTPS `https://<host>/<namespace>/<project>.git`, SSH `git@<host>:<namespace>/<project>.git`.

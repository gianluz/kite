# Git Hooks

Pre-configured Git hooks to maintain code quality.

## Installation

From the repository root:

```bash
./scripts/install-git-hooks.sh
```

This creates symlinks in `.git/hooks/` to the hooks in this directory, so they stay up-to-date automatically.

## Available Hooks

### pre-push

Runs code quality checks before pushing to remote.

**What it does:**

- Builds Kite CLI (if not already built)
- Runs `kite-cli/build/install/kite-cli/bin/kite-cli run quality-checks` (ktlint + detekt)
- Blocks push if checks fail

**Auto-fix style issues:**

```bash
./gradlew ktlintFormat
```

**Bypass hook (not recommended):**

```bash
git push --no-verify
```

## Uninstalling

Remove the symlinks:

```bash
rm .git/hooks/pre-push
```

## Why Symlinks?

Using symlinks instead of copying hooks means:

- ✅ Hooks update automatically when you pull changes
- ✅ Everyone uses the same version
- ✅ No need to reinstall after updates

## Adding New Hooks

1. Create hook script in `scripts/git-hooks/`
2. Make it executable: `chmod +x scripts/git-hooks/your-hook`
3. Run `./scripts/install-git-hooks.sh` to install it
4. Commit both the hook and updated install script

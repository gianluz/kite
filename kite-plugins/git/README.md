# Kite Git Plugin

Type-safe Git operations for Kite workflows.

## Installation

### From Maven Central

```kotlin
@file:DependsOn("io.kite.plugins:git:1.0.0")

import io.kite.plugins.git.*
```

### From Maven Local (Development)

```bash
./gradlew :kite-plugins:git:publishToMavenLocal
```

```kotlin
@file:DependsOnMavenLocal("io.kite.plugins:git:1.0.0-SNAPSHOT")

import io.kite.plugins.git.*
```

### From Local JAR

```bash
./gradlew :kite-plugins:git:build
```

```kotlin
@file:DependsOnJar("./kite-plugins/git/build/libs/git-1.0.0.jar")

import io.kite.plugins.git.*
```

## Usage

### Basic Operations

```kotlin
segments {
    segment("tag-release") {
        execute {
            git {
                // Create tag
                tag("v1.0.0")
                
                // Push tag
                push(tags = true)
            }
        }
    }
}
```

### Tag Operations

```kotlin
git {
    // Simple tag
    tag("v1.0.0")
    
    // Annotated tag with message
    tag("v1.0.0", message = "Release 1.0.0")
    
    // Force create tag (overwrite existing)
    tag("v1.0.0", force = true)
    
    // Check if tag exists
    if (tagExists("v1.0.0")) {
        logger.info("Tag already exists")
    }
    
    // Get latest tag
    val latest = latestTag()
    logger.info("Latest tag: $latest")
}
```

### Push Operations

```kotlin
git {
    // Push current branch
    push()
    
    // Push specific branch
    push(branch = "main")
    
    // Push all tags
    push(tags = true)
    
    // Force push
    push(force = true)
    
    // Push to different remote
    push(remote = "upstream")
}
```

### Remote Operations

```kotlin
git {
    // Fetch from remote
    fetch()
    fetch(remote = "upstream")
    fetch(prune = true)  // Remove deleted remote branches
    
    // Pull changes
    pull()
    pull(remote = "upstream", branch = "main")
    pull(rebase = true)  // Pull with rebase
    
    // Push changes (see Push Operations section for more)
    push()
}
```

### Branch Operations

```kotlin
git {
    // Checkout branch
    checkout("main")
    checkout("feature/new-feature")
    
    // Create and checkout new branch
    checkout("new-branch", createBranch = true)
    
    // Merge branch
    merge("feature/branch")
    merge("hotfix", message = "Merge hotfix")
    merge("feature", fastForward = false)  // Force merge commit
}
```

### Status Checks

```kotlin
git {
    // Get current branch
    val branch = currentBranch()
    logger.info("Current branch: $branch")
    
    // Check if working directory is clean
    if (!isClean()) {
        error("Working directory has uncommitted changes")
    }
    
    // Get modified files
    val modified = modifiedFiles()
    logger.info("Modified files: ${modified.size}")
    
    // Get untracked files
    val untracked = untrackedFiles()
    if (untracked.isNotEmpty()) {
        logger.warn("Untracked files: $untracked")
    }
    
    // Get commit SHA
    val sha = commitSha()
    val shortSha = commitSha(short = true)
    logger.info("Commit: $shortSha")
}
```

### Commit Operations

```kotlin
git {
    // Add files to staging
    add(".")
    add("src/*.kt")
    
    // Commit changes
    commit("Release version 1.0.0")
    
    // Commit all modified files (automatic staging)
    commit("Update documentation", all = true)
}
```

## Complete Examples

### Update and Release Workflow

```kotlin
segments {
    segment("release") {
        execute {
            git {
                // Fetch latest changes
                fetch()
                
                // Switch to main branch
                checkout("main")
                
                // Pull latest changes with rebase
                pull(rebase = true)
                
                // Ensure clean state
                if (!isClean()) {
                    error("Working directory must be clean")
                }
                
                // Merge feature branch
                merge("develop", message = "Merge develop into main")
                
                // Get version
                val version = env("VERSION") ?: error("VERSION not set")
                
                // Create release tag
                tag("v$version", message = "Release $version")
                
                // Push branch and tag
                push(branch = "main")
                push(tags = true)
                
                logger.info("✅ Release v$version created and pushed")
            }
        }
    }
}
```

### Hotfix Workflow

```kotlin
segments {
    segment("hotfix") {
        execute {
            git {
                // Fetch latest
                fetch()
                
                // Create hotfix branch from main
                checkout("main")
                pull(rebase = true)
                checkout("hotfix/critical-bug", createBranch = true)
                
                // Make fixes...
                add(".")
                commit("fix: Critical bug fix")
                
                // Merge back to main
                checkout("main")
                merge("hotfix/critical-bug")
                
                // Tag the hotfix
                tag("v1.0.1", message = "Hotfix 1.0.1")
                
                // Push everything
                push(branch = "main")
                push(tags = true)
                
                logger.info("✅ Hotfix deployed")
            }
        }
    }
}
```

### Version Bump

```kotlin
segments {
    segment("bump-version") {
        execute {
            git {
                // Modify version file
                val versionFile = workspace.resolve("VERSION").toFile()
                versionFile.writeText("1.0.1")
                
                // Commit changes
                add("VERSION")
                commit("chore: Bump version to 1.0.1")
                
                // Push to remote
                push()
                
                logger.info("✅ Version bumped and pushed")
            }
        }
    }
}
```

### Conditional Deployment

```kotlin
segments {
    segment("deploy") {
        condition { ctx ->
            // Only deploy from main branch
            ctx.git {
                currentBranch() == "main" && isClean()
            }
        }
        
        execute {
            // Deployment logic
            logger.info("Deploying from main branch...")
        }
    }
}
```

## API Reference

### Remote Operations

- `fetch(remote: String = "origin", prune: Boolean = false)` - Fetch updates from remote
- `pull(remote: String = "origin", branch: String? = null, rebase: Boolean = false)` - Pull changes from remote
- `push(remote: String = "origin", branch: String? = null, tags: Boolean = false, force: Boolean = false)` - Push
  changes to remote

### Branch Operations

- `currentBranch(): String` - Get current branch name
- `checkout(ref: String, createBranch: Boolean = false)` - Checkout branch, tag, or commit
- `merge(branch: String, message: String? = null, fastForward: Boolean = true)` - Merge branch into current branch

### Tag Operations

- `tag(name: String, message: String? = null, force: Boolean = false)` - Create a tag
- `tagExists(name: String): Boolean` - Check if tag exists
- `latestTag(): String?` - Get latest tag name

### Status Operations

- `isClean(): Boolean` - Check if working directory is clean
- `modifiedFiles(): List<String>` - Get list of modified files
- `untrackedFiles(): List<String>` - Get list of untracked files
- `commitSha(short: Boolean = false): String` - Get current commit SHA

### Commit Operations

- `add(pattern: String = ".")` - Add files to staging area
- `commit(message: String, all: Boolean = false)` - Commit changes

## Requirements

- JGit 6.7.0+
- Kite 0.1.0+
- Git repository initialized in workspace

## License

Apache License 2.0

# Git Plugin

Type-safe Git operations for Kite workflows.

## Overview

The Git plugin provides a type-safe DSL for common Git operations with full support for:

- ✅ Remote operations (fetch, pull, push)
- ✅ Branch management (checkout, merge)
- ✅ Tagging and releases
- ✅ Status checks (isClean, modifiedFiles, etc.)
- ✅ Commit operations

**Perfect for:** CI/CD pipelines, release automation, version management, and automated workflows.

## Installation

```kotlin
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha")

import io.kite.plugins.git.*
```

## Quick Start

```kotlin
segments {
    segment("release") {
        execute {
            git {
                // Ensure clean state
                if (!isClean()) {
                    error("Working directory is not clean")
                }
                
                // Create and push tag
                tag("v1.0.0", message = "Release 1.0.0")
                push(tags = true)
            }
        }
    }
}
```

## Core Features

### Remote Operations

#### Fetch

Update remote-tracking branches without merging:

```kotlin
git {
    // Basic fetch
    fetch()
    
    // Fetch from specific remote
    fetch(remote = "upstream")
    
    // Prune deleted remote branches
    fetch(prune = true)
}
```

#### Pull

Fetch and integrate changes:

```kotlin
git {
    // Pull with merge (default)
    pull()
    
    // Pull with rebase (linear history)
    pull(rebase = true)
    
    // Pull from specific remote/branch
    pull(remote = "origin", branch = "develop")
}
```

#### Push

Push local changes to remote:

```kotlin
git {
    // Push current branch
    push()
    
    // Push specific branch
    push(branch = "main")
    
    // Push to specific remote
    push(remote = "upstream")
    
    // Push tags
    push(tags = true)
    
    // Force push (use with caution!)
    push(force = true)
}
```

### Branch Management

#### Current Branch

```kotlin
git {
    val branch = currentBranch()
    logger.info("Current branch: $branch")
}
```

#### Checkout

```kotlin
git {
    // Switch to existing branch
    checkout("develop")
    
    // Create and switch to new branch
    checkout("feature/new-feature", createBranch = true)
    
    // Checkout specific commit
    checkout("abc123")
}
```

#### Merge

```kotlin
git {
    // Merge with fast-forward (default)
    merge("feature-branch")
    
    // Always create merge commit
    merge("hotfix", fastForward = false)
    
    // Merge with custom message
    merge("develop", message = "Merge develop into main")
}
```

### Status Checks

#### Clean State

```kotlin
git {
    if (isClean()) {
        logger.info("✅ Working directory is clean")
    } else {
        logger.warn("⚠️ There are uncommitted changes")
    }
}
```

#### Modified Files

```kotlin
git {
    val modified = modifiedFiles()
    if (modified.isNotEmpty()) {
        logger.info("Modified files:")
        modified.forEach { logger.info("  - $it") }
    }
}
```

#### Untracked Files

```kotlin
git {
    val untracked = untrackedFiles()
    if (untracked.isNotEmpty()) {
        logger.info("Untracked files:")
        untracked.forEach { logger.info("  - $it") }
    }
}
```

### Tagging

#### Create Tags

```kotlin
git {
    // Lightweight tag
    tag("v1.0.0")
    
    // Annotated tag with message
    tag("v1.0.0", message = "Release 1.0.0")
    
    // Force tag (overwrite existing)
    tag("v1.0.0", force = true)
}
```

#### Check Tags

```kotlin
git {
    if (tagExists("v1.0.0")) {
        logger.info("Tag v1.0.0 already exists")
    }
    
    val latest = latestTag()
    logger.info("Latest tag: $latest")
}
```

### Commit Operations

#### Add Files

```kotlin
git {
    // Add all files
    add(".")
    
    // Add specific file
    add("README.md")
    
    // Add specific pattern
    add("src/**/*.kt")
}
```

#### Commit

```kotlin
git {
    add(".")
    commit("feat: Add new feature")
}
```

#### Commit SHA

```kotlin
git {
    // Full SHA (40 characters)
    val fullSha = commitSha()
    
    // Short SHA (7 characters)
    val shortSha = commitSha(short = true)
    
    logger.info("Current commit: $shortSha")
}
```

## Common Workflows

### Release Workflow

```kotlin
segment("release") {
    execute {
        val version = requireEnv("VERSION")
        
        git {
            // Ensure clean state
            if (!isClean()) {
                error("Cannot release: working directory has uncommitted changes")
            }
            
            // Update from remote
            fetch()
            checkout("main")
            pull(rebase = true)
            
            // Create release tag
            tag("v$version", message = "Release $version")
            
            // Push tag
            push(branch = "main")
            push(tags = true)
            
            logger.info("✅ Released version $version")
        }
    }
}
```

### Hotfix Workflow

```kotlin
segment("hotfix") {
    execute {
        val hotfixVersion = "1.0.1"
        
        git {
            // Start from main
            checkout("main")
            
            // Create hotfix branch
            checkout("hotfix/$hotfixVersion", createBranch = true)
            
            // ... make fixes ...
            add(".")
            commit("fix: Critical bug fix")
            
            // Merge back to main
            checkout("main")
            merge("hotfix/$hotfixVersion", message = "Hotfix $hotfixVersion")
            
            // Tag and push
            tag("v$hotfixVersion")
            push(branch = "main")
            push(tags = true)
        }
    }
}
```

### Feature Branch Workflow

```kotlin
segment("merge-feature") {
    execute {
        val featureBranch = "feature/awesome-feature"
        
        git {
            // Update main
            checkout("main")
            pull(rebase = true)
            
            // Merge feature branch
            merge(featureBranch, message = "Merge awesome feature")
            
            // Push to remote
            push(branch = "main")
            
            logger.info("✅ Feature merged successfully")
        }
    }
}
```

### Pre-commit Validation

```kotlin
segment("validate") {
    execute {
        git {
            val modified = modifiedFiles()
            val untracked = untrackedFiles()
            
            logger.info("Modified files: ${modified.size}")
            logger.info("Untracked files: ${untracked.size}")
            
            if (modified.isEmpty() && untracked.isEmpty()) {
                logger.info("✅ No changes to commit")
                return@execute
            }
            
            // Run tests before committing
            exec("./gradlew", "test")
            
            // Commit if tests pass
            add(".")
            commit("chore: Update files")
            
            logger.info("✅ Changes committed")
        }
    }
}
```

### Continuous Deployment

```kotlin
segment("deploy") {
    condition { isCI && branch == "main" }
    execute {
        git {
            // Get current commit info
            val sha = commitSha(short = true)
            val currentBranch = currentBranch()
            
            logger.info("Deploying from $currentBranch @ $sha")
            
            // Ensure we're on main and up to date
            if (currentBranch != "main") {
                checkout("main")
            }
            pull(rebase = true)
            
            // Tag deployment
            val deployTag = "deploy-${System.currentTimeMillis()}"
            tag(deployTag, message = "Deployment at $sha")
            push(tags = true)
            
            logger.info("✅ Deployment tagged: $deployTag")
        }
    }
}
```

## CI/CD Examples

### GitHub Actions

```kotlin
segment("ci-release") {
    condition { isCI && env("GITHUB_REF")?.startsWith("refs/tags/v") == true }
    execute {
        val tag = env("GITHUB_REF")?.removePrefix("refs/tags/") ?: error("No tag found")
        
        git {
            logger.info("Building release for $tag")
            
            // Verify tag exists locally
            if (!tagExists(tag)) {
                error("Tag $tag does not exist locally")
            }
            
            val sha = commitSha(short = true)
            logger.info("Release $tag @ $sha")
        }
    }
}
```

### GitLab CI

```kotlin
segment("gitlab-deploy") {
    condition { isCI && env("CI_COMMIT_TAG") != null }
    execute {
        val tag = requireEnv("CI_COMMIT_TAG")
        val commitSha = requireEnv("CI_COMMIT_SHA")
        
        git {
            logger.info("Deploying $tag ($commitSha)")
            
            // Tag is already created by GitLab
            // Just verify we're on a clean state
            if (!isClean()) {
                error("Working directory is not clean")
            }
            
            logger.info("✅ Ready to deploy")
        }
    }
}
```

## Complete Example

### Full Release Pipeline with Git + Gradle

```kotlin
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha")
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.git.*
import io.kite.plugins.gradle.*

segments {
    segment("prepare-release") {
        execute {
            val version = requireEnv("VERSION")
            
            git {
                // Verify clean state
                if (!isClean()) {
                    error("Cannot release: uncommitted changes")
                }
                
                // Update from remote
                fetch()
                checkout("main")
                pull(rebase = true)
                
                logger.info("✅ Ready to release $version")
            }
        }
    }
    
    segment("build-and-test") {
        dependsOn("prepare-release")
        execute {
            gradle {
                clean()
                task("build", "test") {
                    parallel = true
                    stacktrace = true
                }
            }
        }
    }
    
    segment("publish") {
        dependsOn("build-and-test")
        execute {
            val version = requireEnv("VERSION")
            
            gradle {
                task("publishToMavenCentral") {
                    property("version", version)
                }
            }
            
            git {
                // Tag release
                tag("v$version", message = "Release $version")
                
                // Push everything
                push(branch = "main")
                push(tags = true)
                
                logger.info("✅ Released $version to Maven Central")
            }
        }
    }
}
```

## Troubleshooting

### Authentication Issues

**Problem:** `remote: Permission denied`

**Solution:** Configure Git credentials or use SSH:

```bash
# Using SSH
git remote set-url origin git@github.com:user/repo.git

# Using token (CI/CD)
git remote set-url origin https://x-access-token:$GITHUB_TOKEN@github.com/user/repo.git
```

### Merge Conflicts

**Problem:** Merge fails with conflicts

**Solution:** The plugin will fail on conflicts. Handle manually or use rebasing:

```kotlin
git {
    pull(rebase = true)  // Rebase instead of merge to avoid conflicts
}
```

### Detached HEAD State

**Problem:** Not on any branch

**Solution:** Checkout a branch explicitly:

```kotlin
git {
    val branch = currentBranch()
    if (branch == "HEAD") {
        checkout("main")
    }
}
```

### Tag Already Exists

**Problem:** `fatal: tag 'v1.0.0' already exists`

**Solution:** Use force flag or check before tagging:

```kotlin
git {
    if (!tagExists("v1.0.0")) {
        tag("v1.0.0")
    } else {
        // Use force to overwrite
        tag("v1.0.0", force = true)
    }
}
```

## API Reference

### GitPlugin

```kotlin
class GitPlugin(private val ctx: ExecutionContext) {
    // Remote operations
    suspend fun fetch(remote: String = "origin", prune: Boolean = false)
    suspend fun pull(remote: String = "origin", branch: String? = null, rebase: Boolean = false)
    suspend fun push(remote: String = "origin", branch: String? = null, tags: Boolean = false, force: Boolean = false)
    
    // Branch management
    fun currentBranch(): String
    suspend fun checkout(ref: String, createBranch: Boolean = false)
    suspend fun merge(branch: String, message: String? = null, fastForward: Boolean = true)
    
    // Status checks
    fun isClean(): Boolean
    fun modifiedFiles(): List<String>
    fun untrackedFiles(): List<String>
    
    // Tagging
    suspend fun tag(name: String, message: String? = null, force: Boolean = false)
    fun tagExists(name: String): Boolean
    fun latestTag(): String?
    
    // Commit operations
    suspend fun add(filePattern: String)
    suspend fun commit(message: String)
    fun commitSha(short: Boolean = false): String
}
```

### Extension Function

```kotlin
suspend fun ExecutionContext.git(configure: suspend GitPlugin.() -> Unit)
```

## See Also

- [Git Documentation](https://git-scm.com/doc)
- [Plugin Development Guide](../dev/05-plugin-development.md)
- [Gradle Plugin](02-plugin-gradle.md)
- [CI Integration](../11-ci-integration.md)

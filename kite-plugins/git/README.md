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

### Release Workflow

```kotlin
segments {
    segment("create-release") {
        execute {
            git {
                // Ensure clean state
                if (!isClean()) {
                    error("Working directory must be clean")
                }
                
                // Get version
                val version = env("VERSION") ?: error("VERSION not set")
                
                // Create release tag
                tag("v$version", message = "Release $version")
                
                // Push tag
                push(tags = true)
                
                logger.info("✅ Release v$version created and pushed")
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

### Tag Operations

- `tag(name: String, message: String? = null, force: Boolean = false)` - Create a tag
- `tagExists(name: String): Boolean` - Check if tag exists
- `latestTag(): String?` - Get latest tag name

### Push Operations

- `push(remote: String = "origin", branch: String? = null, tags: Boolean = false, force: Boolean = false)` - Push
  changes

### Status Operations

- `currentBranch(): String` - Get current branch name
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

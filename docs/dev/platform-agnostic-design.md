# Platform-Agnostic Design Principle

## Design Philosophy

Kite is designed to be **truly platform-agnostic**, meaning it should work on any CI/CD platform without requiring
platform-specific adapters or making assumptions about your workflow.

## Current State vs. Desired State

### ❌ Current Implementation (To Be Refactored)

The current implementation includes platform-specific adapters that populate "magic" properties:

```kotlin
// Current ExecutionContext (opinionated)
data class ExecutionContext(
    val branch: String,
    val commitSha: String,
    val mrNumber: String?,        // ❌ GitLab/GitHub specific
    val isRelease: Boolean,       // ❌ Assumes "release" label convention  
    val isLocal: Boolean,         // ❌ Derived from CI detection
    val ciPlatform: CIPlatform,   // ❌ Why does user need to know?
    val environment: Map<String, String>,
    // ...
)
```

**Problems:**

1. `mrNumber` - Only works for GitLab (`CI_MERGE_REQUEST_IID`) and GitHub (`GITHUB_REF`)
2. `isRelease` - Hard-coded to check for "release" label in GitLab MR labels
3. `ciPlatform` - Forces platform detection when users should just check env vars
4. `isMergeRequest` - Derived from `mrNumber`, breaks for other platforms

### ✅ Desired Implementation (Platform-Agnostic)

Users should query environment variables directly:

```kotlin
// Future ExecutionContext (platform-agnostic)
data class ExecutionContext(
    val branch: String,           // Detected from git/common env vars
    val commitSha: String,        // Detected from git/common env vars
    val environment: Map<String, String>,  // Raw env vars
    val workspace: Path,
    val artifacts: ArtifactManager,
    val logger: SegmentLogger,
)

// Users check platform-specific env vars themselves:
segment("deploy") {
    condition = { ctx ->
        // GitLab: Check MR and labels
        val isGitLabMR = ctx.env("CI_MERGE_REQUEST_IID") != null
        val isRelease = ctx.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
        
        // GitHub: Check PR
        val isGitHubPR = ctx.env("GITHUB_EVENT_NAME") == "pull_request"
        val isRelease = ctx.env("GITHUB_REF")?.contains("/release") == true
        
        // Your logic, your convention
        (isGitLabMR || isGitHubPR) && isRelease
    }
    
    execute {
        exec("./deploy.sh")
    }
}
```

## Benefits of Platform-Agnostic Design

1. **Works Everywhere** - No need for platform-specific adapters
2. **No Assumptions** - Users define their own conventions (release labels, branch names, etc.)
3. **Future-Proof** - New CI platforms work automatically
4. **Transparent** - Users see exactly what environment variables they're checking
5. **Flexible** - Different teams can have different workflows
6. **Plugin-Ready** - Future `kite-gitlab-plugin` or `kite-github-plugin` can add helpers without being in core

## Migration Path

### Step 1: Deprecate Platform-Specific Properties

Mark as `@Deprecated` with clear migration guidance:

```kotlin
@Deprecated(
    message = "Use env(\"CI_MERGE_REQUEST_IID\") or env(\"GITHUB_REF\") instead",
    replaceWith = ReplaceWith("env(\"CI_MERGE_REQUEST_IID\") != null"),
    level = DeprecationLevel.WARNING
)
val mrNumber: String?

@Deprecated(
    message = "Define your own release detection logic using env()",
    replaceWith = ReplaceWith("env(\"CI_MERGE_REQUEST_LABELS\")?.contains(\"release\") == true"),
    level = DeprecationLevel.WARNING
)
val isRelease: Boolean
```

### Step 2: Update Documentation

Show users how to check platform-specific values:

```kotlin
// Check if in GitLab MR
val isGitLabMR = env("CI_MERGE_REQUEST_IID") != null

// Check if in GitHub PR  
val isGitHubPR = env("GITHUB_EVENT_NAME") == "pull_request"

// Check if in Jenkins change request
val isJenkinsPR = env("CHANGE_ID") != null

// Check if in CircleCI PR
val isCirclePR = env("CIRCLE_PULL_REQUEST") != null
```

### Step 3: Remove in v1.0.0

For v1.0.0 release, remove all deprecated properties and platform adapters.

## Future: Optional Platform Plugins

For users who want convenience, we can provide optional platform-specific plugins:

```kotlin
// Optional dependency
dependencies {
    implementation("io.kite:kite-gitlab-plugin:1.0.0")
}

// Provides GitLab-specific helpers
segment("deploy") {
    condition = { ctx ->
        ctx.gitlab.isMergeRequest && 
        ctx.gitlab.hasLabel("release")
    }
}
```

These would be **optional** dependencies, not in core.

## Common Environment Variables Reference

### GitLab CI

- `CI=true` ✅ *Standard*
- `GITLAB_CI=true` ✅ *Platform-specific (used by Kite's isCI detection)*
- `CI_COMMIT_REF_NAME` - Branch name
- `CI_COMMIT_SHA` - Commit SHA
- `CI_MERGE_REQUEST_IID` - MR number
- `CI_MERGE_REQUEST_LABELS` - MR labels (comma-separated)
- `CI_PROJECT_DIR` - Workspace path

### GitHub Actions

- `CI=true` ✅ *Standard*
- `GITHUB_ACTIONS=true` ✅ *Platform-specific (used by Kite's isCI detection)*
- `GITHUB_REF` - Full ref (e.g., `refs/heads/main`, `refs/pull/123/merge`)
- `GITHUB_SHA` - Commit SHA
- `GITHUB_EVENT_NAME` - Event type (`pull_request`, `push`, etc.)
- `GITHUB_WORKSPACE` - Workspace path

### Jenkins

- `CI=true` ✅ *Standard*
- `JENKINS_HOME` - Jenkins installation path ✅ *(used by Kite's isCI detection)*
- `BRANCH_NAME` - Branch name
- `GIT_COMMIT` - Commit SHA
- `CHANGE_ID` - PR/Change number
- `WORKSPACE` - Workspace path

### CircleCI

- `CI=true` ✅ *Standard*
- `CIRCLECI=true` ✅ *Platform-specific (used by Kite's isCI detection)*
- `CIRCLE_BRANCH` - Branch name
- `CIRCLE_SHA1` - Commit SHA
- `CIRCLE_PULL_REQUEST` - PR URL
- `CIRCLE_WORKING_DIRECTORY` - Workspace path

### Travis CI

- `CI=true` ✅ *Standard*
- `TRAVIS=true` ✅ *Platform-specific (used by Kite's isCI detection)*
- `TRAVIS_BRANCH` - Branch name
- `TRAVIS_COMMIT` - Commit SHA
- `TRAVIS_PULL_REQUEST` - PR number (or "false")
- `TRAVIS_BUILD_DIR` - Workspace path

### Buildkite

- `CI=true` ✅ *Standard*
- `BUILDKITE=true` ✅ *Platform-specific (used by Kite's isCI detection)*
- `BUILDKITE_BRANCH` - Branch name
- `BUILDKITE_COMMIT` - Commit SHA
- `BUILDKITE_PULL_REQUEST` - PR number (or "false")
- `BUILDKITE_BUILD_CHECKOUT_PATH` - Workspace path

### TeamCity

- `TEAMCITY_VERSION` - TeamCity version ✅ *(used by Kite's isCI detection)*
- `BUILD_VCS_BRANCH` - Branch name
- `BUILD_VCS_NUMBER` - Commit SHA

## CI Detection: The `isCI` Property

While Kite is platform-agnostic for workflow logic, we provide a helper `isCI` property that reliably
detects if code is running in any CI environment:

```kotlin
val isCI: Boolean
    get() =
        environment["CI"]?.equals("true", ignoreCase = true) == true ||
            environment["GITHUB_ACTIONS"]?.equals("true", ignoreCase = true) == true ||
            environment["GITLAB_CI"]?.equals("true", ignoreCase = true) == true ||
            environment["JENKINS_HOME"] != null ||
            environment["CIRCLECI"]?.equals("true", ignoreCase = true) == true ||
            environment["TRAVIS"]?.equals("true", ignoreCase = true) == true ||
            environment["BUILDKITE"]?.equals("true", ignoreCase = true) == true ||
            environment["TEAMCITY_VERSION"] != null
```

This checks:

1. **`CI=true`** - The de facto standard set by most platforms
2. **Platform-specific indicators** - As a fallback for platforms that don't set `CI=true`

**For custom CI systems:** Simply set `CI=true` in your environment to be detected.

## Implementation Status

- ⏳ **Planned** - To be implemented before v1.0.0
- 📝 **Documented** - Design documented in this file
- 🎯 **Target** - v0.2.0 or v1.0.0 release

---

**Last Updated**: 2024-01-XX
**Status**: Design Document - Not Yet Implemented

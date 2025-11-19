# Phase 4: Platform Adapters

**Status**: ⏭️ **SKIPPED**  
**Goal**: Detect CI platform and populate execution context  
**Duration**: Would have been 1 week

---

## Why Phase 4 Was Skipped

**Original Plan**: Implement platform-specific adapters for GitLab CI, GitHub Actions, and other CI platforms to
automatically detect the environment and populate `ExecutionContext` with platform-specific information.

**Decision**: **SKIP** - Keep Kite platform-agnostic

---

## Original Scope (Not Implemented)

### Epic 4.1: Platform Adapter Framework ⏭️ SKIPPED

Would have included:

- Platform detection system
- Adapter registry
- Priority-based detection

### Epic 4.2: CI Platform Adapters ⏭️ SKIPPED

Would have implemented:

- **GitLabCI adapter**: Read `CI_*` environment variables
- **GitHub Actions adapter**: Read `GITHUB_*` environment variables
- **Local adapter**: Use Git commands
- **Generic adapter**: Fallback for unknown platforms

---

## What We Did Instead

### Generic Approach ✅ IMPLEMENTED

**PlatformAdapter.kt** (242 lines) exists in `kite-core` but:

- Provides **generic interface** only
- Does **not** implement platform-specific logic
- Users access environment variables directly via `ExecutionContext.env()`

**Why This Is Better**:

- ✅ **Platform agnostic** - Works everywhere without special cases
- ✅ **Simpler** - No platform detection logic needed
- ✅ **Flexible** - Users control what they read from environment
- ✅ **Less maintenance** - No need to update for new platforms

### How It Works

**ExecutionContext** provides:

```kotlin
val context = ExecutionContext(
    branch = env("BRANCH_NAME") ?: "unknown",
    commitSha = env("COMMIT_SHA") ?: "unknown",
    mrNumber = env("PR_NUMBER") ?: env("MR_IID"),
    environment = System.getenv(),
    // ...
)
```

**Users can access any environment variable**:

```kotlin
segment("deploy") {
    execute {
        // Platform-agnostic approach
        val prNumber = env("PR_NUMBER")         // GitHub
            ?: env("MR_IID")                    // GitLab
            ?: env("PULL_REQUEST_ID")           // Other platform
        
        val branch = requireEnv("BRANCH_NAME")  // Whatever platform uses
    }
}
```

### CI Platform Enum

We kept a simple **CIPlatform enum** for informational purposes:

- `GITLAB`
- `GITHUB`
- `LOCAL`
- `GENERIC`

But we **don't auto-detect** it. Users can set it manually if needed, but it's not required.

---

## Benefits of Skipping Phase 4

### 1. Simplicity

- No complex platform detection logic
- No platform-specific code paths
- Easier to understand and maintain

### 2. Flexibility

- Works with **any** CI platform (Jenkins, CircleCI, Travis, Bamboo, etc.)
- Users control environment variable mapping
- No need to wait for Kite to support a new platform

### 3. Less Code

- Saved ~500 lines of platform-specific code
- Saved ~300 lines of platform-specific tests
- Fewer files to maintain

### 4. Platform Agnostic

- True to Kite's philosophy
- No vendor lock-in
- Works anywhere with environment variables

---

## What Users Do Instead

**Before** (if we had platform adapters):

```kotlin
// Kite would auto-detect platform and populate context
val context = PlatformDetector.detect()  // GitLab vs GitHub vs etc.
```

**Now** (platform-agnostic):

```kotlin
// Users read environment variables directly
segment("build") {
    execute {
        val prNumber = env("PR_NUMBER") ?: env("MR_IID")
        val branch = env("BRANCH_NAME") ?: env("GITHUB_REF")
        
        if (prNumber != null) {
            println("Building PR/MR: $prNumber")
        }
    }
}
```

**Result**: More flexible, simpler, works everywhere.

---

## What Stayed from Phase 4

### ExecutionContext Properties

We still have platform-agnostic properties in `ExecutionContext`:

- `branch: String` - Git branch (user provides)
- `commitSha: String` - Git commit SHA (user provides)
- `mrNumber: String?` - MR/PR number (user provides)
- `isRelease: Boolean` - Release flag (user provides)
- `isLocal: Boolean` - Local vs CI (user provides)
- `ciPlatform: CIPlatform` - Platform enum (user provides)

But users populate these manually, not via platform adapters.

### PlatformAdapter Interface

The **PlatformAdapter.kt** file (242 lines) exists in `kite-core` but:

- It's a **generic interface**
- No platform-specific implementations
- Kept for future extensibility if needed

---

## Could We Add Platform Adapters Later?

**Yes**, but probably not needed:

**If users request it**:

- Could add as optional feature in v1.1.0+
- Would be **opt-in**, not required
- Generic approach would still work

**But current approach is sufficient**:

- Users can write helper functions
- Can share common logic in files
- Platform-specific logic stays in user's code

---

## Comparison

| Aspect | With Platform Adapters | Without (Current) |
|--------|----------------------|------------------|
| **Complexity** | High | Low |
| **Code** | +800 lines | Current |
| **Flexibility** | Limited to supported platforms | Works everywhere |
| **Maintenance** | Must update for new platforms | No updates needed |
| **User Control** | Less | More |
| **CI Agnostic** | No | Yes |

---

## Conclusion

**Phase 4 was correctly skipped**. The platform-agnostic approach is:

- ✅ Simpler
- ✅ More flexible
- ✅ Less code to maintain
- ✅ Works with any CI platform
- ✅ True to Kite's philosophy

Users get **more control** and Kite stays **platform agnostic**.

---

## Next Steps

Phase 4 is **SKIPPED** ⏭️

**Next**: Phase 5 - Built-in Helpers & Features

See [devplan/README.md](README.md) for overall progress.

---

**Last Updated**: November 18, 2025  
**Status**: ⏭️ Skipped - Platform agnostic approach chosen  
**Saved**: ~800 lines of code, ongoing maintenance

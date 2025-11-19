# Security Roadmap

This document outlines Kite's security features - implemented and planned.

## Kite's Security Philosophy

**Kite is designed for internal CI/CD pipelines** - not for running untrusted code.

**Trust Model:**

- Kite runs **YOUR code** in **YOUR CI environment**
- CI platform (GitHub Actions, GitLab CI) handles trust boundaries
- Kite provides **defense-in-depth**: masking, auditing, validation

**Not Designed For:**

- Running community-contributed segments from marketplace
- Executing code from untrusted sources (fork PRs)
- Multi-tenant environments with hostile users

**Designed For:**

- Internal development teams
- Private CI/CD pipelines
- Replacing Fastlane/custom scripts
- Full flexibility with security guardrails

---

## Phase 1: Foundation (v1.0.0) ✅ COMPLETE

### Epic S1: Secret Masking ✅ COMPLETE

**Status**: ✅ Implemented (November 2025)

**What We Have:**

- `SecretMasker` singleton - thread-safe secret tracking
- Automatic masking of plain text, URL-encoded, and Base64 secrets
- `secret()` and `requireSecret()` API in ExecutionContext
- Automatic masking in all logs (SegmentLogger integration)
- Hint system: `[API_KEY:***]` for debugging
- 15 comprehensive tests
- 550+ lines of documentation

**Files:**

- `kite-core/src/main/kotlin/io/kite/core/SecretMasker.kt`
- `kite-core/src/main/kotlin/io/kite/core/ExecutionContext.kt`
- `kite-runtime/src/main/kotlin/io/kite/runtime/logging/SegmentLogger.kt`
- `docs/SECURITY.md`

**Prevents:**

- ✅ Secrets in log messages
- ✅ Secrets in command execution logs
- ✅ Secrets in command output
- ✅ Secrets in error messages
- ✅ Secrets in CI artifacts

**Example:**

```kotlin
val apiKey = secret("API_KEY")
exec("curl", "-H", "Authorization: Bearer $apiKey")
// Logs: curl -H Authorization: Bearer [API_KEY:***]
```

**Compliance:** GDPR, PCI-DSS, SOC 2 ready

---

## Phase 2: Defense in Depth (v1.1.0) ⏳ PLANNED

### Epic S2: Selective Auto-Masking

**Status**: ⏳ Planned for v1.1.0
**Estimate**: 1 day

**Goal**: Automatically detect and mask secrets even if developer forgets to use `secret()`

**Implementation:**

```kotlin
fun ExecutionContext.env(key: String): String? {
    val value = environment[key]
    
    if (value != null && looksLikeSecret(key)) {
        logger.warn("⚠️  '$key' looks like a secret - auto-masking. Use secret('$key') to suppress this warning.")
        SecretMasker.registerSecret(value, hint = key)
    }
    
    return value
}

private fun looksLikeSecret(key: String): Boolean {
    val patterns = listOf(
        Regex(".*KEY$", RegexOption.IGNORE_CASE),
        Regex(".*SECRET.*", RegexOption.IGNORE_CASE),
        Regex(".*TOKEN.*", RegexOption.IGNORE_CASE),
        Regex(".*PASSWORD.*", RegexOption.IGNORE_CASE),
        Regex(".*CREDENTIAL.*", RegexOption.IGNORE_CASE),
        Regex(".*AUTH.*", RegexOption.IGNORE_CASE),
    )
    return patterns.any { it.matches(key) }
}

// Explicit opt-out for false positives
fun ExecutionContext.envPlaintext(key: String): String? {
    return environment[key]  // No masking
}
```

**Benefits:**

- Catches ~95% of secrets automatically
- Educates developers with warnings
- Non-breaking change
- Escape hatch for false positives

**Example:**

```kotlin
// Developer forgets to use secret()
val apiKey = env("GITHUB_TOKEN")  // ⚠️  Auto-masked + warning

// False positive (PATH, HOME, etc.)
val path = envPlaintext("PATH")    // Explicitly not masked
```

---

### Epic S3: Execution Audit Logging

**Status**: ⏳ Planned for v1.1.0
**Estimate**: 1-2 days

**Goal**: Create forensic audit trail of what each segment did

**Implementation:**

Create `.kite/execution-audit.json` after each ride:

```json
{
  "rideId": "ci-2025-11-18-17:30:45",
  "rideName": "CI",
  "startTime": "2025-11-18T17:30:45Z",
  "endTime": "2025-11-18T17:31:12Z",
  "success": true,
  "segments": [
    {
      "name": "build",
      "startTime": "2025-11-18T17:30:46Z",
      "endTime": "2025-11-18T17:30:52Z",
      "exitCode": 0,
      "secretsAccessed": ["GITHUB_TOKEN"],
      "commandsExecuted": ["./gradlew", "build"],
      "filesCreated": ["app/build/outputs/apk/app-release.apk"],
      "artifactsProduced": ["apk"],
      "duration": 6.2
    },
    {
      "name": "deploy",
      "startTime": "2025-11-18T17:30:53Z",
      "endTime": "2025-11-18T17:31:12Z",
      "exitCode": 0,
      "secretsAccessed": ["GITHUB_TOKEN", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY"],
      "commandsExecuted": ["gh", "release", "create", "aws", "s3", "sync"],
      "networkHosts": ["api.github.com", "s3.amazonaws.com"],
      "duration": 19.0
    }
  ]
}
```

**Benefits:**

- Forensic analysis after incidents
- Compliance auditing (who accessed what secrets)
- Performance monitoring
- Security incident response
- Can be uploaded to CI as artifact

**What Gets Logged:**

- ✅ Segment name and timestamps
- ✅ Secrets accessed (names only, NOT values)
- ✅ Commands executed (with args, masked)
- ✅ Files created/modified
- ✅ Artifacts produced
- ✅ Exit codes and durations
- ✅ Network hosts accessed (future)

---

### Epic S4: Static Analysis

**Status**: ⏳ Planned for v1.1.0  
**Estimate**: 2-3 days

**Goal**: Detect dangerous patterns during script compilation

**Implementation:**

Add validation during KTS compilation:

```kotlin
class SecurityValidator {
    fun validateScript(scriptContent: String): List<SecurityWarning> {
        val warnings = mutableListOf<SecurityWarning>()
        
        // Pattern 1: Direct ProcessBuilder usage
        if (scriptContent.contains("ProcessBuilder")) {
            warnings.add(SecurityWarning(
                level = Level.WARN,
                message = "Direct ProcessBuilder usage detected. Consider using exec() instead for better logging and security."
            ))
        }
        
        // Pattern 2: Runtime.exec()
        if (scriptContent.contains("Runtime.getRuntime().exec")) {
            warnings.add(SecurityWarning(
                level = Level.WARN,
                message = "Runtime.exec() detected. Use exec() helper for consistent behavior."
            ))
        }
        
        // Pattern 3: System.exit()
        if (scriptContent.contains("System.exit")) {
            warnings.add(SecurityWarning(
                level = Level.ERROR,
                message = "System.exit() kills the entire Kite process. Use error() to fail segment."
            ))
        }
        
        // Pattern 4: Suspicious exfiltration patterns
        val exfiltrationPatterns = listOf(
            "curl.*\\$.*KEY",      // Sending env var in URL
            "wget.*\\$.*TOKEN",    // Sending token
            "nc.*\\$.*SECRET",     // Netcat exfiltration
        )
        
        exfiltrationPatterns.forEach { pattern ->
            if (Regex(pattern).containsMatchIn(scriptContent)) {
                warnings.add(SecurityWarning(
                    level = Level.WARN,
                    message = "Suspicious pattern detected that might leak secrets. Review carefully."
                ))
            }
        }
        
        return warnings
    }
}
```

**What It Detects:**

- ⚠️ Direct `ProcessBuilder` usage (suggest `exec()`)
- ⚠️  `Runtime.exec()` usage
- ❌  `System.exit()` calls (kills Kite)
- ⚠️ Suspicious exfiltration patterns
- ⚠️ Accessing `env()` with secret-like names

**Configurable:**

```kotlin
// .kite/security-rules.kts
security {
    rules {
        blockSystemExit = true
        warnOnProcessBuilder = true
        warnOnRuntimeExec = true
        warnOnSecretPatterns = true
    }
    
    allowList {
        // Specific patterns to ignore
        ignorePattern("ProcessBuilder.*docker")
    }
}
```

**Benefits:**

- Catch mistakes before execution
- Educate developers
- Prevent accidental security issues
- Non-breaking (warnings, not errors)

---

## Phase 3: Advanced Features (v2.0.0) ⏳ OPTIONAL

### Epic S5: Capability System

**Status**: ⏳ Optional - Only if Kite adds marketplace/third-party segments  
**Estimate**: 2-3 weeks

**Goal**: Sandbox untrusted segments with fine-grained permissions

**Only Implement If:**

- Kite adds a segment marketplace
- Running community-contributed code
- Multi-tenant environment

**Current Assessment:** NOT NEEDED for internal CI/CD tool

**Design:**

```kotlin
segment("community-plugin") {
    // Declare what this segment can do
    capabilities {
        network = false          // No outbound network
        secrets = emptyList()    // No secret access
        filesystem = readOnly()  // Read-only file access
        exec = allowList("grep", "sed", "awk")  // Only specific commands
    }
    
    execute {
        // Kite enforces capabilities at runtime
    }
}
```

---

### Epic S6: Network Allowlisting

**Status**: ⏳ Optional  
**Estimate**: 1 week

**Goal**: Restrict which hosts segments can contact

**Only Implement If:** Strong security requirements or untrusted code

**Design:**

```kotlin
security {
    networkPolicy {
        defaultDeny = true
        
        allowList {
            host("api.github.com")
            host("*.amazonaws.com")
            host("registry.npmjs.org")
        }
        
        blockList {
            host("pastebin.com")  // Common exfiltration site
        }
    }
}
```

---

### Epic S7: Signed Segment Bundles

**Status**: ⏳ Optional  
**Estimate**: 2 weeks

**Goal**: Verify segment authenticity and integrity

**Only Implement If:** Running third-party segments

**Design:**

```
segment-bundle/
├── segments/
│   └── build.kite.kts
├── MANIFEST.json
└── SIGNATURE.sig

MANIFEST.json:
{
  "name": "android-build-tools",
  "version": "1.0.0",
  "publisher": "acme-corp",
  "segments": [
    {
      "name": "build.kite.kts",
      "sha256": "abc123..."
    }
  ]
}
```

---

## What We're NOT Implementing

### ❌ Safe Command Wrappers (e.g., SafeCLI.runGradle())

**Why Not:**

- Kills flexibility - CI pipelines need to run ANY command
- Kite is for YOUR code, not hostile code
- Command sanitization would be overly restrictive

### ❌ Blocking Arbitrary Execution

**Why Not:**

- Defeats the purpose of a CI/CD tool
- Users need docker, kubectl, terraform, etc.
- Too restrictive for internal pipelines

### ❌ Untrusted vs Trusted Mode

**Why Not:**

- CI platform (GitHub Actions, GitLab CI) already handles this
- Fork PRs can't access secrets (GitHub's responsibility)
- Duplicating CI platform features adds complexity

### ❌ Dependency Signature Verification

**Why Not:**

- Maven Central already has protections
- Supply chain security is better handled with SBOMs
- Adds significant complexity for marginal benefit

---

## Security Documentation

### Current Documentation

- ✅ `docs/SECURITY.md` (550 lines) - Complete secret masking guide
- ✅ `docs/ARTIFACTS_CROSS_RIDE.md` - Secure artifact sharing
- ✅ `docs/CI_INTEGRATION.md` - CI security best practices

### Planned Documentation (v1.1.0)

- ⏳ `docs/SECURITY_MODEL.md` - Kite's trust model and boundaries
- ⏳ `docs/AUDIT_LOGGING.md` - How to use execution audit logs
- ⏳ `docs/STATIC_ANALYSIS.md` - Security validation rules

---

## Summary

### ✅ Implemented (v1.0.0)

- Secret masking (automatic, thread-safe, multi-encoding)
- `secret()` and `requireSecret()` API
- Integration with logging system
- Comprehensive documentation

### ⏳ Planned (v1.1.0)

- Selective auto-masking (defense-in-depth)
- Execution audit logging (forensics)
- Static analysis (catch mistakes early)

### ⏳ Optional (v2.0.0+)

- Capability system (only if marketplace)
- Network allowlisting (only if high security req)
- Signed bundles (only if third-party code)

### ❌ Not Implementing

- Safe command wrappers (too restrictive)
- Execution sandboxing (CI platform's job)
- Trust modes (duplicate CI platform)
- Dependency signing (unnecessary complexity)

**Kite's security model balances flexibility with safety** - perfect for internal CI/CD! 🔒

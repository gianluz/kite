# Security Roadmap

This document outlines Kite's security features - implemented and planned. Security is a cross-phase concern that
evolves with the project.

## Security Philosophy

**Kite's Trust Model:**

- Designed for **internal pipelines** (YOUR code, YOUR team)
- Not designed for untrusted/community code
- CI platform provides trust boundaries (secrets, network isolation)
- Kite provides **defense-in-depth** (masking, auditing, validation)

**Key Principle**: Security should be **easy to do right** and **hard to do wrong**.

---

## Phase 1: Foundation Security (v1.0.0)

### Epic 5.6: Secret Management ✅ COMPLETE

**Status**: ✅ Complete (November 2025)  
**Story Points**: 8 | **Duration**: 2 days

#### Tasks

- [x] **Task 5.6.1**: Implement SecretMasker ✅
    - Created thread-safe `SecretMasker` singleton (111 lines)
    - Uses `ConcurrentHashMap` for thread safety
    - Supports plain text, URL-encoded, Base64-encoded secrets
    - Hint system for debugging: `[API_KEY:***]`
    - Zero configuration required
    - **Deliverable**: `kite-core/src/main/kotlin/io/kite/core/SecretMasker.kt`
    - **Tests**: 15 comprehensive tests in `SecretMaskerTest.kt`

- [x] **Task 5.6.2**: Add Secret API to ExecutionContext ✅
    - Added `secret(key)` - Get env var and auto-register as secret
    - Added `requireSecret(key)` - Required secret with validation
    - Integrated with existing `env()` method
    - **Deliverable**: Updated `ExecutionContext.kt`
    - **Tests**: Integrated in SecretMaskerTest

- [x] **Task 5.6.3**: Integrate with Logging System ✅
    - Updated `SegmentLogger` to automatically mask all messages
    - Masks command execution logs
    - Masks command output
    - Masks error messages
    - **Deliverable**: Updated `SegmentLogger.kt`
    - **Tests**: Verified in integration tests

- [x] **Task 5.6.4**: Document Security Best Practices ✅
    - Comprehensive security guide (550+ lines)
    - Real-world examples (GitHub, Docker, Database)
    - Common pitfalls and how to avoid them
    - CI/CD integration patterns
    - Compliance considerations (GDPR, PCI-DSS, SOC 2)
    - **Deliverable**: `docs/SECURITY.md`

#### Deliverables

✅ **Production Code**:

- `SecretMasker.kt` - 111 lines
- Updated `ExecutionContext.kt`
- Updated `SegmentLogger.kt`
- **Total**: ~350 lines

✅ **Tests**: 15 tests in `SecretMaskerTest.kt` - all passing

✅ **Documentation**: `docs/SECURITY.md` - 550+ lines

✅ **Features**:

- Automatic secret masking in all logs
- Simple API: `secret("KEY")` instead of `env("KEY")`
- Zero-config security
- Thread-safe and performant
- Prevents leaks in:
    - Log messages
    - Command execution
    - Command output
    - Error messages

#### What This Protects Against

✅ **Accidental leaks** - Secrets automatically masked in logs  
✅ **Copy-paste errors** - Can't copy secret from logs  
✅ **CI artifacts** - Log files don't contain secrets  
✅ **Compliance violations** - Audit-ready logs

---

## Phase 2: Enhanced Security (v1.1.0) - PLANNED

### Epic: Selective Auto-Masking 🔄 PLANNED

**Status**: 🔄 Planned for v1.1.0  
**Story Points**: 5 | **Duration**: 1 day

#### Tasks

- [ ] **Task: Pattern-based auto-detection**
    - Detect secret-like environment variables automatically
    - Patterns: `*KEY*`, `*SECRET*`, `*TOKEN*`, `*PASSWORD*`, `*CREDENTIAL*`, `*AUTH*`
    - Auto-mask + warn when accessed via `env()`
    - Add `envPlaintext()` for explicit opt-out
    - **Goal**: Catch 95% of secrets without explicit registration

- [ ] **Task: Update documentation**
    - Document auto-masking behavior
    - Explain pattern matching
    - Show how to opt-out for false positives

#### Why This Matters

**Problem**: Developers might forget to use `secret()` and use `env()` directly

```kotlin
val apiKey = env("API_KEY")  // ❌ Could leak!
```

**Solution**: Auto-detect and mask automatically

```kotlin
val apiKey = env("API_KEY")  // ✅ Auto-masked + warning logged
// Output: ⚠️  Auto-masking 'API_KEY' - use secret() to suppress this warning
```

#### Deliverables

- Pattern-based detection in `env()`
- Warning messages for auto-masked variables
- `envPlaintext()` escape hatch
- Updated `docs/SECURITY.md`

---

### Epic: Execution Audit Log 🔄 PLANNED

**Status**: 🔄 Planned for v1.1.0  
**Story Points**: 3 | **Duration**: 1 day

#### Tasks

- [ ] **Task: Implement audit log**
    - Create `.kite/execution-audit.json` after each ride
    - Record per-segment:
        - Segment name, start/end time, duration
        - Secrets accessed (names only, not values)
        - Commands executed
        - Exit codes
    - Thread-safe append-only log
    - **Goal**: Full provenance tracking for compliance

- [ ] **Task: Add audit log documentation**
    - Document audit log format
    - Show how to use for compliance
    - Integration with SIEM systems

#### Example Audit Log

```json
{
  "rideId": "ci-2025-11-18-001",
  "startTime": "2025-11-18T17:00:00Z",
  "endTime": "2025-11-18T17:05:23Z",
  "segments": [
    {
      "name": "build",
      "startTime": "2025-11-18T17:00:00Z",
      "endTime": "2025-11-18T17:02:15Z",
      "duration": 135.4,
      "secretsAccessed": [],
      "commandsExecuted": ["./gradlew", "build"],
      "exitCode": 0
    },
    {
      "name": "deploy",
      "startTime": "2025-11-18T17:02:15Z",
      "endTime": "2025-11-18T17:05:23Z",
      "duration": 188.2,
      "secretsAccessed": ["GITHUB_TOKEN", "AWS_SECRET_KEY"],
      "commandsExecuted": ["gh", "release", "create", "aws", "s3", "sync"],
      "exitCode": 0
    }
  ]
}
```

#### Why This Matters

✅ **Compliance**: Proves what secrets were accessed when  
✅ **Forensics**: Investigate security incidents  
✅ **Auditing**: Track all command executions  
✅ **Monitoring**: Detect unusual patterns

#### Deliverables

- `ExecutionAuditLog.kt` class
- JSON audit log generation
- Integration with schedulers
- Documentation in `docs/SECURITY.md`

---

## Phase 3: Static Analysis (v1.2.0) - PLANNED

### Epic: Script Validation 🔄 PLANNED

**Status**: 🔄 Planned for v1.2.0  
**Story Points**: 8 | **Duration**: 2-3 days

#### Tasks

- [ ] **Task: AST-based script scanning**
    - Scan compiled KTS scripts for dangerous patterns
    - Check during script compilation phase
    - Provide warnings or errors before execution
    - **Integration Point**: `KiteScriptCompiler`

- [ ] **Task: Dangerous pattern detection**
    - Detect: `System.exit()` - crashes entire process
    - Detect: `Runtime.exec()` - suggest `exec()` instead
    - Detect: Direct `ProcessBuilder` usage
    - Detect: Reflection accessing private fields
    - Detect: File writes outside workspace
    - Detect: Network calls bypassing allowed hosts

- [ ] **Task: Configurable validation rules**
    - Allow projects to define custom rules
    - Configuration in `.kite/security.yml`
    - Severity levels: `error`, `warn`, `info`

#### Example Validation

```kotlin
// ❌ Detected: Direct ProcessBuilder usage
segment("deploy") {
    execute {
        ProcessBuilder("rm", "-rf", "/").start()  // ERROR
    }
}

// ✅ Suggested: Use safe exec() wrapper
segment("deploy") {
    execute {
        exec("rm", "-rf", workspaceRoot)  // OK - validated path
    }
}
```

#### Why This Matters

✅ **Catch mistakes early** - Before execution  
✅ **Educate developers** - Suggest better patterns  
✅ **Prevent accidents** - Block dangerous operations

#### Deliverables

- `ScriptValidator.kt` with AST scanning
- Pattern detection rules
- Integration with compiler
- Configuration system
- Documentation

---

## Phase 4: Enterprise Features (v2.0.0) - FUTURE

These features are **not planned for v1.x** but could be added if there's enterprise demand.

### Epic: Capability System 🚫 NOT PLANNED

**Status**: 🚫 Deferred - only if Kite becomes a marketplace  
**Story Points**: 20+ | **Duration**: 2-3 weeks

**What it would include:**

- Capability declarations: `capabilities = [network, secrets, fileWrite]`
- Sandbox enforcement (restricted file access, network allowlist)
- Trusted vs untrusted mode (for PRs from forks)
- Signed segment bundles for third-party code

**Why we're NOT doing this now:**

- Kite is for **internal pipelines**, not hostile code
- CI platform already provides trust boundaries
- Would significantly complicate the architecture
- No user demand yet

---

### Epic: Safe Command Wrappers 🚫 NOT PLANNED

**Status**: 🚫 Won't implement - conflicts with Kite's philosophy

**What others suggest:**

- Replace `exec()` with safe wrappers: `SafeCLI.runGradle()`, `SafeCLI.runGit()`
- Sanitize all arguments automatically
- Block arbitrary command execution

**Why we're NOT doing this:**

- **Flexibility is a feature** - Users need to run ANY command
- CI pipelines need `docker`, `kubectl`, `terraform`, custom tools
- Wrappers would be incomplete and frustrating
- Kite is NOT GitHub Actions (doesn't run community code)

**Our approach instead:** Provide good security primitives (masking, auditing) and let users be flexible.

---

## Security Checklist

### ✅ v1.0.0 (November 2025)

- [x] Secret masking in all logs
- [x] `secret()` and `requireSecret()` API
- [x] Thread-safe secret storage
- [x] Multiple encoding support (plain, URL, Base64)
- [x] Comprehensive security documentation
- [x] 15 security tests

### 🔄 v1.1.0 (Planned)

- [ ] Pattern-based auto-masking
- [ ] Execution audit log
- [ ] Warning for likely secrets accessed via `env()`
- [ ] `envPlaintext()` escape hatch

### 🔄 v1.2.0 (Planned)

- [ ] Static analysis of scripts
- [ ] Dangerous pattern detection
- [ ] Configurable validation rules
- [ ] Pre-execution security checks

### 🚫 v2.0.0+ (Not Planned Unless Needed)

- [ ] Capability system
- [ ] Safe command wrappers
- [ ] Sandbox enforcement
- [ ] Signed bundles

---

## Comparison: Kite vs GitHub Actions Security

| Feature | GitHub Actions | Kite (v1.0) | Kite (v1.1) | Kite (v2.0+) |
|---------|----------------|-------------|-------------|--------------|
| **Trust Model** | Untrusted code | Internal only | Internal only | Configurable |
| **Secret Masking** | ✅ Automatic | ✅ Automatic | ✅ Enhanced | ✅ Enhanced |
| **Audit Logging** | ✅ Built-in | ❌ None | ✅ Added | ✅ Enhanced |
| **Static Analysis** | ❌ None | ❌ None | ✅ Added | ✅ Enhanced |
| **Capabilities** | ✅ Strict | ❌ None | ❌ None | 🔄 Optional |
| **Sandbox** | ✅ VM-level | ❌ None | ❌ None | 🔄 Optional |

---

## Recommendations

### For v1.0.0 Users (Now)

✅ **Use `secret()` for all sensitive data**

```kotlin
val apiKey = secret("API_KEY")  // ✅ Masked automatically
```

✅ **Check logs to verify masking**

```
[10:23:45.123] [deploy] $ curl -H Authorization: Bearer [GITHUB_TOKEN:***]
```

✅ **Review `docs/SECURITY.md` for best practices**

### For v1.1.0 Users (Soon)

✅ **Leverage auto-masking** - Most secrets caught automatically  
✅ **Review audit logs** - Track secret access for compliance  
✅ **Fix warnings** - Address auto-masking warnings

### For v1.2.0 Users (Future)

✅ **Enable static analysis** - Catch mistakes before execution  
✅ **Configure custom rules** - Project-specific security policies

---

## Key Insights

### What Makes Sense for Kite

✅ **Secret masking** - Easy win, huge value  
✅ **Audit logging** - Compliance and forensics  
✅ **Static analysis** - Catch mistakes early  
✅ **Good documentation** - Help users do it right

### What Doesn't Fit

❌ **Capability system** - Over-engineered for internal use  
❌ **Safe wrappers** - Kills flexibility  
❌ **Sandbox** - CI platform's job  
❌ **Trust modes** - Not Kite's concern

### Bottom Line

**Kite provides defense-in-depth for internal pipelines, not a security fortress for hostile code.**

The goal is to make it **easy to be secure** without sacrificing the **flexibility** that makes Kite useful.

---

## References

- **Documentation**: `docs/SECURITY.md` - Complete security guide
- **Implementation**: `kite-core/src/main/kotlin/io/kite/core/SecretMasker.kt`
- **Tests**: `kite-core/src/test/kotlin/io/kite/core/SecretMaskerTest.kt`
- **Examples**: See `docs/SECURITY.md` for real-world patterns

---

**Last Updated**: November 18, 2025  
**Version**: v1.0.0  
**Status**: Phase 1 Complete, Phase 2-3 Planned

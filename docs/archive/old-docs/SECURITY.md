## Security - Secret Management

Comprehensive guide to handling secrets securely in Kite.

## 🔐 The Problem: Secrets Leakage

**Secrets in logs are a critical security vulnerability**:

```kotlin
// ❌ DANGEROUS - Secret exposed in logs!
val apiKey = env("API_KEY")  // "sk-1234567890"
exec("curl", "-H", "Authorization: Bearer $apiKey")
// Logs show: Authorization: Bearer sk-1234567890 ⚠️
```

**Consequences**:

- API keys exposed in CI logs
- Passwords visible in artifacts
- Tokens leaked in error messages
- Compliance violations (GDPR, PCI-DSS, etc.)

---

## ✅ The Solution: Automatic Secret Masking

Kite provides **automatic secret masking** with zero configuration:

```kotlin
// ✅ SAFE - Secret automatically masked!
val apiKey = secret("API_KEY")  // Registers as secret
exec("curl", "-H", "Authorization: Bearer $apiKey")
// Logs show: Authorization: Bearer [API_KEY:***] ✅
```

---

## 🎯 Quick Start

### 1. Use `secret()` Instead of `env()`

```kotlin
segment("deploy") {
    execute {
        // ❌ Don't use env() for secrets
        val token = env("GITHUB_TOKEN")
        
        // ✅ Use secret() - automatically masked
        val token = secret("GITHUB_TOKEN")
        
        exec("gh", "auth", "login", "--with-token", token)
        // Logs: gh auth login --with-token [GITHUB_TOKEN:***]
    }
}
```

### 2. Use `requireSecret()` for Required Secrets

```kotlin
segment("deploy") {
    execute {
        // Throws error if not set, and masks the value
        val apiKey = requireSecret("API_KEY")
        val dbPassword = requireSecret("DB_PASSWORD")
        
        exec("deploy-script", "--api-key", apiKey, "--password", dbPassword)
        // Logs: deploy-script --api-key [API_KEY:***] --password [DB_PASSWORD:***]
    }
}
```

---

## 📖 API Reference

### ExecutionContext Methods

#### `secret(key: String): String?`

Gets an environment variable and automatically registers it as a secret.

```kotlin
val apiKey = secret("API_KEY")
```

**What it does**:

- Reads the environment variable
- Registers the value for masking
- Returns the value (or null if not set)
- Masks in ALL logs and outputs

**When to use**: For optional secrets

#### `requireSecret(key: String): String`

Gets a required secret, throws if not set.

```kotlin
val token = requireSecret("GITHUB_TOKEN")
```

**What it does**:

- Reads the environment variable
- Throws `IllegalArgumentException` if not set
- Registers the value for masking
- Returns the non-null value

**When to use**: For required secrets

### SecretMasker Methods

#### `registerSecret(value: String, hint: String?)`

Manually registers a value as a secret.

```kotlin
SecretMasker.registerSecret("my-api-key", hint = "API_KEY")
```

**What it masks**:

- The exact value
- URL-encoded version
- Base64-encoded version

**Use case**: Secrets not from environment variables

#### `mask(text: String, showHints: Boolean = true): String`

Masks all secrets in the given text.

```kotlin
val output = command.execute()
val safe = SecretMasker.mask(output, showHints = true)
```

**Parameters**:

- `showHints` = `true`: Shows `[API_KEY:***]`
- `showHints` = `false`: Shows just `***`

---

## 🔍 What Gets Masked

### Automatic Masking Locations

Secrets are **automatically masked** in:

1. **✅ All log messages**
   ```kotlin
   logger.info("Using API key: $apiKey")
   // Logs: Using API key: [API_KEY:***]
   ```

2. **✅ Command execution**
   ```kotlin
   exec("curl", "-H", "Authorization: Bearer $token")
   // Logs: curl -H Authorization: Bearer [GITHUB_TOKEN:***]
   ```

3. **✅ Command output**
   ```kotlin
   val result = exec("cat", "config.json")
   // If config.json contains secrets, they're masked in logs
   ```

4. **✅ Error messages**
   ```kotlin
   try {
       exec("deploy", "--key", apiKey)
   } catch (e: Exception) {
       logger.error("Deployment failed: ${e.message}")
       // Exception messages are masked
   }
   ```

### Encoded Versions

Kite automatically masks **multiple encodings**:

```kotlin
val secret = secret("MY_SECRET")  // Value: "my+password"

// All of these are masked:
// 1. Plain text: my+password
// 2. URL-encoded: my%2Bpassword  
// 3. Base64: bXkrcGFzc3dvcmQ=
```

---

## 💡 Best Practices

### 1. Always Use `secret()` for Sensitive Data

```kotlin
// ✅ Good
val apiKey = secret("API_KEY")
val password = requireSecret("DB_PASSWORD")
val token = secret("GITHUB_TOKEN")

// ❌ Bad - will leak in logs!
val apiKey = env("API_KEY")
val password = System.getenv("DB_PASSWORD")
```

### 2. Register Secrets Early

Register secrets as soon as you read them:

```kotlin
segment("setup") {
    execute {
        // Register all secrets at the start
        val apiKey = secret("API_KEY")
        val dbPassword = secret("DB_PASSWORD")
        val token = secret("GITHUB_TOKEN")
        
        // Now all commands/logs are safe
        // ... rest of segment
    }
}
```

### 3. Don't Print Secrets in Custom Messages

```kotlin
// ❌ Bad - before registering as secret
val key = env("API_KEY")
println("API Key is: $key")  // Leaks!

// ✅ Good - register first
val key = secret("API_KEY")
println("API Key is: $key")  // Masked: [API_KEY:***]
```

### 4. Use Hints for Debugging

Hints help identify which secret was masked:

```kotlin
SecretMasker.registerSecret(value, hint = "STRIPE_SECRET_KEY")
// Logs show: [STRIPE_SECRET_KEY:***]
// Easy to identify which secret is being used
```

### 5. Validate Secrets Exist

```kotlin
segment("deploy") {
    execute {
        // Use requireSecret for critical secrets
        val apiKey = requireSecret("API_KEY")
        // Throws clear error if not set: "Required secret environment variable 'API_KEY' is not set"
        
        // Or check manually
        val optionalKey = secret("OPTIONAL_KEY")
        if (optionalKey == null) {
            logger.warn("OPTIONAL_KEY not set, using defaults")
        }
    }
}
```

---

## 🔒 Real-World Examples

### Example 1: GitHub Deployment

```kotlin
segment("deploy-to-github") {
    execute {
        val githubToken = requireSecret("GITHUB_TOKEN")
        val githubRepo = env("GITHUB_REPOSITORY") ?: "owner/repo"
        
        // Token is masked in all logs
        exec("gh", "auth", "login", "--with-token", githubToken)
        exec("gh", "release", "create", "v1.0.0", "--repo", githubRepo)
        
        logger.info("Deployment successful!")
        // Logs safe even if commands output the token
    }
}
```

### Example 2: Database Connection

```kotlin
segment("run-migrations") {
    execute {
        val dbHost = env("DB_HOST") ?: "localhost"
        val dbUser = env("DB_USER") ?: "postgres"
        val dbPassword = requireSecret("DB_PASSWORD")  // Masked!
        
        val connectionString = "postgresql://$dbUser:$dbPassword@$dbHost/mydb"
        // Connection string is masked: postgresql://postgres:[DB_PASSWORD:***]@localhost/mydb
        
        exec("psql", connectionString, "-f", "migrations.sql")
    }
}
```

### Example 3: Multiple API Keys

```kotlin
segment("sync-services") {
    execute {
        // Register all secrets upfront
        val stripeKey = requireSecret("STRIPE_SECRET_KEY")
        val twilioKey = requireSecret("TWILIO_AUTH_TOKEN")
        val sendgridKey = requireSecret("SENDGRID_API_KEY")
        
        // All commands are safe
        exec("sync-stripe", "--api-key", stripeKey)
        exec("sync-twilio", "--token", twilioKey)
        exec("sync-sendgrid", "--key", sendgridKey)
        
        // Logs show:
        // sync-stripe --api-key [STRIPE_SECRET_KEY:***]
        // sync-twilio --token [TWILIO_AUTH_TOKEN:***]
        // sync-sendgrid --key [SENDGRID_API_KEY:***]
    }
}
```

### Example 4: Docker Login

```kotlin
segment("docker-push") {
    execute {
        val dockerUser = env("DOCKER_USERNAME") ?: error("DOCKER_USERNAME not set")
        val dockerPassword = requireSecret("DOCKER_PASSWORD")
        val imageName = "myapp:latest"
        
        // Password is masked in logs
        exec("docker", "login", "-u", dockerUser, "-p", dockerPassword)
        exec("docker", "push", imageName)
        
        logger.info("Image pushed successfully")
    }
}
```

---

## 🚨 Common Pitfalls

### Pitfall 1: Reading Secret Before Registering

```kotlin
// ❌ Wrong order
val key = env("API_KEY")              // Not registered yet
println("Using key: $key")             // LEAKS!
SecretMasker.registerSecret(key)      // Too late

// ✅ Correct order
val key = secret("API_KEY")           // Registered immediately
println("Using key: $key")             // Masked: [API_KEY:***]
```

### Pitfall 2: Not Masking Derived Values

```kotlin
val password = secret("DB_PASSWORD")
val hash = password.hashCode()

// ❌ Hash might appear in logs unmask
println("Password hash: $hash")        // Not masked!

// ✅ Register derived values too
SecretMasker.registerSecret(hash.toString(), hint = "DB_PASSWORD_HASH")
```

### Pitfall 3: Secrets in Exceptions

```kotlin
// ❌ Don't include secrets in exceptions before masking
val apiKey = env("API_KEY")
throw Exception("Failed with key: $apiKey")  // LEAKS!

// ✅ Register first, or don't include in message
val apiKey = secret("API_KEY")
throw Exception("Failed with key: $apiKey")  // Masked
// Or better: don't include the key at all
throw Exception("API authentication failed")
```

---

## 🧪 Testing Secret Masking

### Test Your Secret Masking

```kotlin
@Test
fun `secrets are masked in logs`() {
    SecretMasker.clear()
    SecretMasker.registerSecret("my-api-key", hint = "API_KEY")
    
    val text = "Using API key: my-api-key"
    val masked = SecretMasker.mask(text)
    
    assertFalse(masked.contains("my-api-key"))
    assertTrue(masked.contains("[API_KEY:***]"))
}
```

### Verify in Integration Tests

```kotlin
@Test
fun `segment execution masks secrets`() {
    // Set secret in environment
    System.setProperty("API_KEY", "secret-123")
    
    val segment = segment("test") {
        execute {
            val key = secret("API_KEY")
            logger.info("Key is: $key")
        }
    }
    
    val result = runSegment(segment)
    val logs = result.logs
    
    // Verify secret is masked
    assertFalse(logs.contains("secret-123"))
    assertTrue(logs.contains("[API_KEY:***]"))
}
```

---

## 📋 Checklist: Secure Secret Handling

Before deploying, verify:

- [ ] All secrets use `secret()` or `requireSecret()`
- [ ] No `env()` calls for sensitive data
- [ ] Secrets registered before any logging
- [ ] Tested secret masking in logs
- [ ] CI artifacts don't contain secrets
- [ ] Error messages don't leak secrets
- [ ] Derived values (hashes, tokens) also masked if needed

---

## 🔧 Advanced Usage

### Custom Secret Registration

```kotlin
segment("advanced") {
    execute {
        // Read from file
        val keyFile = workspace.resolve("secret.key").toFile()
        val apiKey = keyFile.readText().trim()
        SecretMasker.registerSecret(apiKey, hint = "FILE_API_KEY")
        
        // Generated values
        val sessionToken = generateSessionToken(apiKey)
        SecretMasker.registerSecret(sessionToken, hint = "SESSION_TOKEN")
        
        // All are now masked
        exec("curl", "-H", "Authorization: Bearer $sessionToken")
    }
}
```

### Batch Registration

```kotlin
segment("setup-secrets") {
    execute {
        // Register multiple secrets at once
        val secrets = mapOf(
            "AWS_ACCESS_KEY" to env("AWS_ACCESS_KEY_ID")!!,
            "AWS_SECRET_KEY" to env("AWS_SECRET_ACCESS_KEY")!!,
            "AWS_SESSION_TOKEN" to env("AWS_SESSION_TOKEN")
        ).filterValues { it != null }
        
        SecretMasker.registerSecrets(secrets)
        
        // All AWS credentials now masked
    }
}
```

### Clear Secrets (Testing Only)

```kotlin
// Only in tests - clears all registered secrets
SecretMasker.clear()
```

---

## 🌐 CI/CD Integration

### GitHub Actions

Secrets are automatically masked in GitHub Actions logs, but Kite provides additional protection:

```yaml
- name: Deploy
  env:
    API_KEY: ${{ secrets.API_KEY }}
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
  run: kite-cli ride DEPLOY
```

**Benefits**:

- GitHub masks `${{ secrets.X }}` in workflow logs
- Kite masks in segment logs
- Double protection against leaks

### GitLab CI

```yaml
deploy:
  script:
    - kite ride DEPLOY
  variables:
    API_KEY: $CI_API_KEY
    DB_PASSWORD: $DB_PASSWORD_SECRET
```

**Benefits**:

- GitLab masks protected variables
- Kite masks in segment logs and artifacts

---

## 📊 Summary

| Feature | Benefit |
|---------|---------|
| **Automatic Masking** | Zero configuration, works everywhere |
| **Multiple Encodings** | Masks plain, URL-encoded, Base64 |
| **Hint System** | Easy debugging with `[API_KEY:***]` |
| **Thread-Safe** | Safe for parallel execution |
| **Zero Overhead** | Only masks when secrets registered |

**Bottom line**: Use `secret()` instead of `env()` for sensitive data. Kite handles the rest. 🔒

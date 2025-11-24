# Secret Management

Learn how to handle sensitive data securely in your Kite workflows.

---

## The Problem

**Secrets in logs are a critical security vulnerability:**

```kotlin
// ❌ DANGEROUS - Secret exposed in logs!
val apiKey = env("API_KEY")  // "sk-1234567890"
exec("curl", "-H", "Authorization: Bearer $apiKey")
// Logs show: Authorization: Bearer sk-1234567890 ⚠️
```

**Consequences:**

- API keys exposed in CI logs
- Passwords visible in build artifacts
- Tokens leaked in error messages
- Compliance violations (GDPR, PCI-DSS, SOC 2)
- Security breaches

---

## The Solution

Kite provides **automatic secret masking** with zero configuration:

```kotlin
// ✅ SAFE - Secret automatically masked!
val apiKey = secret("API_KEY")  // Registers as secret
exec("curl", "-H", "Authorization: Bearer $apiKey")
// Logs show: Authorization: Bearer [API_KEY:***] ✅
```

---

## Quick Start

### Use `secret()` Instead of `env()`

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

### Use `requireSecret()` for Required Secrets

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

## API Reference

### `secret(key: String): String?`

Gets an environment variable and automatically registers it as a secret.

```kotlin
val apiKey = secret("API_KEY")
```

**Behavior:**

- Reads the environment variable `key`
- Registers the value for automatic masking
- Returns the value (or `null` if not set)
- Masks the secret in ALL logs, command output, and error messages

**Use case:** Optional secrets

**Example:**

```kotlin
val optionalKey = secret("ANALYTICS_KEY")
if (optionalKey != null) {
    exec("send-analytics", "--key", optionalKey)
    // Logs: send-analytics --key [ANALYTICS_KEY:***]
} else {
    logger.info("Analytics disabled (no key provided)")
}
```

### `requireSecret(key: String): String`

Gets a required secret, throws if not set.

```kotlin
val token = requireSecret("GITHUB_TOKEN")
```

**Behavior:**

- Reads the environment variable `key`
- Throws `IllegalArgumentException` if not set or empty
- Registers the value for automatic masking
- Returns the non-null value

**Use case:** Required secrets that must be present

**Example:**

```kotlin
val deployToken = requireSecret("DEPLOY_TOKEN")
exec("deploy", "--token", deployToken)
// Logs: deploy --token [DEPLOY_TOKEN:***]
```

**Error message (if not set):**

```
IllegalArgumentException: Required secret environment variable 'DEPLOY_TOKEN' is not set
```

---

## What Gets Masked

### Automatic Masking Locations

Secrets are **automatically masked** in:

**1. Log messages:**

```kotlin
val apiKey = secret("API_KEY")
logger.info("Using API key: $apiKey")
// Logs: Using API key: [API_KEY:***]
```

**2. Command execution:**

```kotlin
val token = secret("GITHUB_TOKEN")
exec("curl", "-H", "Authorization: Bearer $token", "https://api.github.com")
// Logs: curl -H Authorization: Bearer [GITHUB_TOKEN:***] https://api.github.com
```

**3. Command output:**

```kotlin
val password = secret("DB_PASSWORD")
val result = exec("psql", "-U", "user", "-c", "SELECT * FROM users")
// If command outputs the password, it's masked in logs
```

**4. Error messages:**

```kotlin
try {
    val key = secret("API_KEY")
    exec("deploy", "--key", key)
} catch (e: Exception) {
    logger.error("Deployment failed: ${e.message}")
    // Exception messages have secrets masked
}
```

### Encoded Versions

Kite automatically masks **multiple encodings** of your secret:

```kotlin
val secret = secret("MY_SECRET")  // Value: "my+password"

// All of these are automatically masked:
// 1. Plain text:    my+password
// 2. URL-encoded:   my%2Bpassword  
// 3. Base64:        bXkrcGFzc3dvcmQ=
```

This protects against secrets appearing in:

- HTTP headers (often Base64-encoded)
- URLs (URL-encoded)
- API requests
- Configuration files

---

## Best Practices

### ✅ DO

**1. Always use `secret()` for sensitive data:**

```kotlin
// ✅ Good
val apiKey = secret("API_KEY")
val password = requireSecret("DB_PASSWORD")
val token = secret("GITHUB_TOKEN")
```

**2. Register secrets early:**

```kotlin
segment("setup") {
    execute {
        // Register all secrets at the start
        val apiKey = secret("API_KEY")
        val dbPassword = secret("DB_PASSWORD")
        val token = secret("GITHUB_TOKEN")
        
        // Now all commands and logs are safe
        // ... rest of segment
    }
}
```

**3. Use `requireSecret()` for critical secrets:**

```kotlin
segment("deploy") {
    execute {
        // Fails fast with clear error if not set
        val deployKey = requireSecret("DEPLOY_KEY")
        
        exec("deploy", "--key", deployKey)
    }
}
```

**4. Validate secrets before use:**

```kotlin
val apiKey = secret("API_KEY")
if (apiKey.isNullOrBlank()) {
    error("API_KEY is required for deployment")
}

// Or just use requireSecret()
val apiKey = requireSecret("API_KEY")
```

### ❌ DON'T

**1. Don't use `env()` for secrets:**

```kotlin
// ❌ Bad - will leak in logs!
val apiKey = env("API_KEY")
val password = System.getenv("DB_PASSWORD")

// ✅ Good
val apiKey = secret("API_KEY")
val password = secret("DB_PASSWORD")
```

**2. Don't print secrets before registering:**

```kotlin
// ❌ Bad - leaks before registration
val key = env("API_KEY")
println("API Key is: $key")  // LEAKS!
SecretMasker.registerSecret(key)  // Too late

// ✅ Good - register first
val key = secret("API_KEY")
println("API Key is: $key")  // Masked: [API_KEY:***]
```

**3. Don't include secrets in custom error messages:**

```kotlin
// ❌ Bad
val key = env("API_KEY")
throw Exception("Failed with key: $key")  // LEAKS!

// ✅ Good - register first
val key = secret("API_KEY")
throw Exception("Failed with key: $key")  // Masked

// ✅ Better - don't include the key
throw Exception("API authentication failed")
```

---

## Real-World Examples

### Example 1: GitHub Deployment

```kotlin
segment("deploy-release") {
    description = "Deploy to GitHub Releases"
    
    execute {
        val githubToken = requireSecret("GITHUB_TOKEN")
        val repo = env("GITHUB_REPOSITORY") ?: "owner/repo"
        val version = env("VERSION") ?: "v1.0.0"
        
        // Token is automatically masked in all logs
        exec("gh", "auth", "login", "--with-token", githubToken)
        exec("gh", "release", "create", version, 
            "--repo", repo,
            "--title", "Release $version",
            "dist/*.jar"
        )
        
        logger.info("Successfully deployed $version")
        // Safe even if commands output the token
    }
}
```

**Console output:**

```
gh auth login --with-token [GITHUB_TOKEN:***]
gh release create v1.0.0 --repo owner/repo --title Release v1.0.0 dist/*.jar
Successfully deployed v1.0.0
```

### Example 2: Database Migrations

```kotlin
segment("run-migrations") {
    description = "Run database migrations"
    
    execute {
        val dbHost = env("DB_HOST") ?: "localhost"
        val dbUser = env("DB_USER") ?: "postgres"
        val dbPassword = requireSecret("DB_PASSWORD")  // Masked!
        val dbName = env("DB_NAME") ?: "production"
        
        val connectionString = "postgresql://$dbUser:$dbPassword@$dbHost/$dbName"
        // Connection string is masked: postgresql://postgres:[DB_PASSWORD:***]@localhost/production
        
        logger.info("Connecting to database...")
        exec("flyway", "migrate", "-url=$connectionString")
        logger.info("Migrations completed")
    }
}
```

**Console output:**

```
Connecting to database...
flyway migrate -url=postgresql://postgres:[DB_PASSWORD:***]@localhost/production
Migrations completed
```

### Example 3: Multi-Cloud Deployment

```kotlin
segment("deploy-multicloud") {
    description = "Deploy to AWS, Azure, and GCP"
    
    execute {
        // Register all cloud secrets
        val awsKey = requireSecret("AWS_ACCESS_KEY_ID")
        val awsSecret = requireSecret("AWS_SECRET_ACCESS_KEY")
        val azureKey = requireSecret("AZURE_CLIENT_SECRET")
        val gcpKey = requireSecret("GCP_SERVICE_ACCOUNT_KEY")
        
        // Deploy to AWS
        exec("aws", "configure", "set", "aws_access_key_id", awsKey)
        exec("aws", "configure", "set", "aws_secret_access_key", awsSecret)
        exec("aws", "s3", "cp", "app.zip", "s3://my-bucket/")
        
        // Deploy to Azure
        exec("az", "login", "--service-principal",
            "--username", "myapp",
            "--password", azureKey,
            "--tenant", env("AZURE_TENANT_ID")!!
        )
        exec("az", "webapp", "deploy", "--name", "myapp")
        
        // Deploy to GCP
        exec("gcloud", "auth", "activate-service-account",
            "--key-file", "-",  // stdin
            stdin = gcpKey  // Kite masks stdin too
        )
        exec("gcloud", "app", "deploy")
        
        logger.info("Multi-cloud deployment complete!")
    }
}
```

**All credentials are automatically masked in logs!**

### Example 4: Docker Build and Push

```kotlin
segment("docker-push") {
    description = "Build and push Docker image"
    
    execute {
        val dockerUser = env("DOCKER_USERNAME") ?: error("DOCKER_USERNAME required")
        val dockerPassword = requireSecret("DOCKER_PASSWORD")
        val imageName = "myapp:${env("VERSION") ?: "latest"}"
        
        // Build
        exec("docker", "build", "-t", imageName, ".")
        
        // Login - password is masked
        exec("docker", "login", "-u", dockerUser, "-p", dockerPassword)
        // Logs: docker login -u john -p [DOCKER_PASSWORD:***]
        
        // Push
        exec("docker", "push", imageName)
        
        logger.info("Image $imageName pushed successfully")
    }
}
```

---

## Advanced Usage

### Manual Secret Registration

For secrets not from environment variables:

```kotlin
segment("custom-secrets") {
    execute {
        // Read secret from file
        val keyFile = workspace.resolve("secrets/api.key").toFile()
        val apiKey = keyFile.readText().trim()
        SecretMasker.registerSecret(apiKey, hint = "FILE_API_KEY")
        
        // Generated secrets
        val sessionToken = generateToken(apiKey)
        SecretMasker.registerSecret(sessionToken, hint = "SESSION_TOKEN")
        
        // Both are now masked everywhere
        exec("curl", "-H", "Authorization: Bearer $sessionToken", "https://api.example.com")
    }
}
```

### Batch Registration

```kotlin
segment("setup-aws") {
    execute {
        // Register multiple secrets at once
        val awsSecrets = mapOf(
            "AWS_ACCESS_KEY" to requireSecret("AWS_ACCESS_KEY_ID"),
            "AWS_SECRET_KEY" to requireSecret("AWS_SECRET_ACCESS_KEY"),
            "AWS_SESSION_TOKEN" to secret("AWS_SESSION_TOKEN") ?: ""
        ).filterValues { it.isNotEmpty() }
        
        SecretMasker.registerSecrets(awsSecrets)
        
        // All AWS credentials now masked
        exec("aws", "s3", "ls")
    }
}
```

### Conditional Secrets

```kotlin
segment("deploy") {
    execute {
        val environment = env("ENVIRONMENT") ?: "dev"
        
        val secretKey = when (environment) {
            "production" -> requireSecret("PROD_API_KEY")
            "staging" -> requireSecret("STAGING_API_KEY")
            else -> secret("DEV_API_KEY") ?: "dev-key-123"
        }
        
        // All variants are masked
        exec("deploy", "--env", environment, "--key", secretKey)
    }
}
```

---

## CI/CD Integration

### GitHub Actions

GitHub Actions automatically masks secrets from `${{ secrets.X }}`, and Kite provides additional protection:

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy with Kite
        env:
          API_KEY: ${{ secrets.API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./kite-cli/build/install/kite-cli/bin/kite-cli run deploy
```

**Double protection:**

- GitHub masks `${{ secrets.X }}` in workflow logs
- Kite masks in segment logs and outputs
- Secrets never visible anywhere

### GitLab CI

```yaml
deploy:
  stage: deploy
  script:
    - kite-cli/build/install/kite-cli/bin/kite-cli run deploy
  variables:
    API_KEY: $CI_API_KEY
    DB_PASSWORD: $DB_PASSWORD_SECRET
  only:
    - main
```

**Benefits:**

- GitLab masks protected/masked variables
- Kite provides additional masking in logs
- Safe artifact generation

### Jenkins

```groovy
pipeline {
    agent any
    environment {
        API_KEY = credentials('api-key-id')
        DB_PASSWORD = credentials('db-password-id')
    }
    stages {
        stage('Deploy') {
            steps {
                sh './kite-cli/build/install/kite-cli/bin/kite-cli run deploy'
            }
        }
    }
}
```

**Protection:**

- Jenkins masks credentials
- Kite adds extra masking layer
- Multiple safeguards

---

## Troubleshooting

### Problem: Secret still appearing in logs

**Cause:** Using `env()` instead of `secret()`

**Solution:**

```kotlin
// ❌ Wrong
val key = env("API_KEY")

// ✅ Correct
val key = secret("API_KEY")
```

---

### Problem: "Required secret not set" error

**Symptom:**

```
IllegalArgumentException: Required secret environment variable 'API_KEY' is not set
```

**Solutions:**

1. **Set the environment variable:**

```bash
export API_KEY=your-key-here
kite-cli/build/install/kite-cli/bin/kite-cli run deploy
```

2. **Use `secret()` for optional secrets:**

```kotlin
// Instead of requireSecret():
val apiKey = requireSecret("API_KEY")

// Use secret() with null check:
val apiKey = secret("API_KEY") 
    ?: error("API_KEY is required for this operation")

// Or provide a default:
val apiKey = secret("API_KEY") ?: "default-key"
```

---

### Problem: Derived values not masked

**Example:**

```kotlin
val password = secret("DB_PASSWORD")
val hash = password.hashCode()
println("Hash: $hash")  // Not masked!
```

**Solution:**
Register derived values too:

```kotlin
val password = secret("DB_PASSWORD")
val hash = password.hashCode().toString()
SecretMasker.registerSecret(hash, hint = "DB_PASSWORD_HASH")
println("Hash: $hash")  // Now masked: [DB_PASSWORD_HASH:***]
```

---

## Testing

### Test Secret Masking

```kotlin
@Test
fun `secrets are masked in logs`() {
    SecretMasker.clear()  // Clear for test isolation
    SecretMasker.registerSecret("my-api-key", hint = "API_KEY")
    
    val text = "Using API key: my-api-key"
    val masked = SecretMasker.mask(text)
    
    assertFalse(masked.contains("my-api-key"))
    assertTrue(masked.contains("[API_KEY:***]"))
}

@Test
fun `masked secrets in encoded forms`() {
    SecretMasker.clear()
    SecretMasker.registerSecret("test+secret", hint = "TEST")
    
    val urlEncoded = "test%2Bsecret"
    val base64 = "dGVzdCtzZWNyZXQ="  // Base64("test+secret")
    
    assertTrue(SecretMasker.mask(urlEncoded).contains("[TEST:***]"))
    assertTrue(SecretMasker.mask(base64).contains("[TEST_BASE64:***]"))
}
```

### Integration Testing

```kotlin
@Test
fun `segment execution masks secrets`() {
    // Set test secret
    System.setProperty("TEST_SECRET", "secret-123")
    
    val segment = segment("test") {
        execute {
            val key = secret("TEST_SECRET")
            logger.info("Key is: $key")
        }
    }
    
    // Capture logs
    val logCapture = LogCapture()
    runSegment(segment, logCapture)
    
    // Verify masking
    val logs = logCapture.getLogs()
    assertFalse(logs.contains("secret-123"))
    assertTrue(logs.contains("[TEST_SECRET:***]"))
}
```

---

## Security Checklist

Before deploying to production:

- [ ] All secrets use `secret()` or `requireSecret()`
- [ ] No `env()` or `System.getenv()` calls for sensitive data
- [ ] Secrets registered before any logging or command execution
- [ ] Tested secret masking in unit/integration tests
- [ ] Verified CI artifacts don't contain secrets
- [ ] Checked error messages don't leak secrets
- [ ] Reviewed command outputs for secret exposure
- [ ] Derived values (hashes, tokens) also masked if needed
- [ ] Team trained on secret handling practices

---

## Summary

**Key Points:**

| Feature | Benefit |
|---------|---------|
| **`secret(key)`** | Automatic masking, returns nullable |
| **`requireSecret(key)`** | Automatic masking + validation |
| **Automatic encoding** | Masks plain, URL-encoded, and Base64 |
| **Hint system** | Shows `[API_KEY:***]` for easy debugging |
| **Thread-safe** | Safe for parallel segment execution |
| **Zero overhead** | Only processes when secrets are registered |

**Bottom line:**  
Use `secret()` or `requireSecret()` instead of `env()` for sensitive data. Kite handles the rest. 🔒

---

## Related Topics

- [Execution Context](06-execution-context.md) - Complete API reference including secret methods
- [CI Integration](11-ci-integration.md) - Using secrets in GitHub Actions, GitLab CI, etc.
- [Writing Segments](04-writing-segments.md) - Best practices for segment design

---

## Next Steps

- [Learn about external dependencies →](10-external-dependencies.md)
- [Integrate with CI/CD →](11-ci-integration.md)
- [Explore CLI reference →](12-cli-reference.md)

# External Dependencies

Learn how to use external libraries in your Kite scripts.

---

## Overview

Kite supports using external libraries (Gson, OkHttp, Apache Commons, etc.) in your `.kite.kts` scripts using **two
methods**:

1. **`@DependsOn` Annotation** - For standalone, portable scripts
2. **Classpath Dependencies** - For project-integrated workflows

Both approaches are fully supported with IDE autocomplete and work in CLI + CI/CD environments.

---

## Method 1: @DependsOn Annotation

**Best for:** Standalone scripts, quick prototyping, shareable workflows

### Quick Example

```kotlin
// .kite/segments/json-processing.kite.kts
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson

segments {
    segment("process-json") {
        description = "Process JSON data"
        execute {
            val gson = Gson()
            
            val data = mapOf(
                "project" to "Kite",
                "version" to "1.0.0"
            )
            
            val json = gson.toJson(data)
            logger.info("JSON: $json")
        }
    }
}
```

**What happens:**

1. Kite sees `@file:DependsOn` annotation
2. Downloads `gson-2.10.1.jar` from Maven Central using Apache Ivy
3. Adds it to the classpath automatically
4. Your script can import and use Gson

**First run:** Slower (downloading dependencies)  
**Subsequent runs:** Fast (dependencies cached in `~/.ivy2/cache`)

### Multiple Dependencies

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("org.apache.commons:commons-lang3:3.14.0")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")

import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import okhttp3.OkHttpClient

segments {
    segment("multi-lib") {
        execute {
            val gson = Gson()
            val capitalized = StringUtils.capitalize("hello")
            val client = OkHttpClient()
            
            logger.info("All libraries available!")
        }
    }
}
```

### Custom Repositories

```kotlin
@file:DependsOn("com.example:custom-lib:1.0.0")
@file:Repository("https://repo.example.com/maven2/")

import com.example.CustomLib

segments {
    segment("use-custom-lib") {
        execute {
            val lib = CustomLib()
            // Use it
        }
    }
}
```

**Default repository:** Maven Central (`https://repo.maven.apache.org/maven2/`)

---

## Method 2: Classpath Dependencies

**Best for:** Production builds, centralized dependency management, version catalogs

### Step 1: Add to build.gradle.kts

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.kite:kite-dsl:0.1.0")
    
    // Add libraries you want to use in Kite scripts
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

### Step 2: Use in Your Script

```kotlin
// .kite/segments/json-processing.kite.kts
// No @DependsOn needed - already on classpath!

import com.google.gson.Gson

segments {
    segment("process-json") {
        execute {
            val gson = Gson()
            
            val data = mapOf("project" to "Kite")
            logger.info("JSON: ${gson.toJson(data)}")
        }
    }
}
```

**Benefits:**

- ✅ Faster (no runtime resolution)
- ✅ Centralized version management
- ✅ Works with version catalogs
- ✅ Better for CI/CD (reproducible builds)

---

## Comparison

| Feature | @DependsOn | Classpath |
|---------|-----------|-----------|
| **Setup** | None | Add to build.gradle.kts |
| **Portability** | ✅ Standalone | ⚠️ Needs project |
| **Speed (first run)** | Slower (downloads) | Fast |
| **Speed (subsequent)** | Fast (cached) | Fast |
| **IDE Autocomplete** | ✅ After first run | ✅ Immediate |
| **Version Management** | Per-script | Centralized |
| **Best For** | Standalone scripts | Project builds |

---

## Common Use Cases

### JSON Processing with Gson

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

segment("api-response") {
    execute {
        val gson = Gson()
        
        // Serialize
        val data = mapOf("status" to "success", "code" to 200)
        val json = gson.toJson(data)
        logger.info("Response: $json")
        
        // Deserialize
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val parsed = gson.fromJson<Map<String, Any>>(json, type)
        logger.info("Parsed: $parsed")
    }
}
```

### HTTP Requests with OkHttp

```kotlin
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")

import okhttp3.OkHttpClient
import okhttp3.Request

segment("fetch-data") {
    execute {
        val client = OkHttpClient()
        
        val request = Request.Builder()
            .url("https://api.github.com/repos/octocat/Hello-World")
            .header("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        logger.info("Status: ${response.code}")
        logger.info("Body: ${response.body?.string()}")
    }
}
```

### String Utilities with Apache Commons

```kotlin
@file:DependsOn("org.apache.commons:commons-lang3:3.14.0")

import org.apache.commons.lang3.StringUtils

segment("format-text") {
    execute {
        val text = "hello world"
        
        val capitalized = StringUtils.capitalize(text)
        logger.info("Capitalized: $capitalized")  // "Hello world"
        
        val abbreviated = StringUtils.abbreviate("This is a long text", 10)
        logger.info("Abbreviated: $abbreviated")  // "This is..."
        
        val stripped = StringUtils.strip("  hello  ")
        logger.info("Stripped: '$stripped'")  // "hello"
    }
}
```

### XML Processing with Jackson

```kotlin
@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.16.0")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

segment("parse-xml") {
    execute {
        val xmlMapper = XmlMapper().registerKotlinModule()
        
        val xmlString = """
            <project>
                <name>Kite</name>
                <version>1.0.0</version>
            </project>
        """.trimIndent()
        
        data class Project(val name: String, val version: String)
        val project = xmlMapper.readValue(xmlString, Project::class.java)
        
        logger.info("Project: ${project.name} v${project.version}")
    }
}
```

### Database Access with JDBC

```kotlin
@file:DependsOn("org.postgresql:postgresql:42.7.1")

import java.sql.DriverManager

segment("query-database") {
    execute {
        val dbPassword = requireSecret("DB_PASSWORD")
        val url = "jdbc:postgresql://localhost:5432/mydb"
        
        val connection = DriverManager.getConnection(url, "user", dbPassword)
        
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")
        
        if (resultSet.next()) {
            val count = resultSet.getInt(1)
            logger.info("User count: $count")
        }
        
        connection.close()
    }
}
```

---

## Complete Working Example

### File Structure

```
.kite/
├── segments/
│   └── api-client.kite.kts
└── rides/
    └── api-workflow.kite.kts
```

### Segment File

```kotlin
// .kite/segments/api-client.kite.kts
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

segments {
    segment("fetch-repos") {
        description = "Fetch GitHub repositories"
        execute {
            val client = OkHttpClient()
            val gson = Gson()
            
            val request = Request.Builder()
                .url("https://api.github.com/users/octocat/repos")
                .header("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty response")
            
            // Parse JSON array
            @Suppress("UNCHECKED_CAST")
            val repos = gson.fromJson(body, List::class.java) as List<Map<String, Any>>
            
            logger.info("Found ${repos.size} repositories")
            repos.take(5).forEach { repo ->
                logger.info("  - ${repo["name"]}: ${repo["description"]}")
            }
        }
    }
    
    segment("process-repos") {
        description = "Process repository data"
        dependsOn("fetch-repos")
        execute {
            logger.info("Processing completed repositories")
            // Further processing...
        }
    }
}
```

### Ride File

```kotlin
// .kite/rides/api-workflow.kite.kts
ride {
    name = "API Workflow"
    
    flow {
        segment("fetch-repos")
        segment("process-repos")
    }
}
```

### Running

```bash
$ kite ride "API Workflow"

  ██╗  ██╗██╗████████╗███████╗
  ██║ ██╔╝██║╚══██╔══╝██╔════╝
  █████╔╝ ██║   ██║   █████╗  
  ██╔═██╗ ██║   ██║   ██╔══╝  
  ██║  ██╗██║   ██║   ███████╗
  ╚═╝  ╚═╝╚═╝   ╚═╝   ╚══════╝

▶ Executing Ride: API Workflow

Found 26 repositories
  - Hello-World: My first repository on GitHub!
  - octocat.github.io: GitHub Pages site
  - Spoon-Knife: This repo is for demonstration purposes only
  - git-consortium: Example repo
  - test-repo1: Test repository 1

✓ fetch-repos (1.2s)
✓ process-repos (12ms)

🎉 All segments completed successfully!
```

---

## IDE Support

### IntelliJ IDEA Setup

**With @DependsOn:**

1. Write your script with `@file:DependsOn` annotations
2. Run once: `kite ride "My Ride"`
3. Dependencies download to `~/.ivy2/cache`
4. In IntelliJ: **File → Reload All Gradle Projects**
5. Autocomplete now works!

**With Classpath Dependencies:**

1. Add dependencies to `build.gradle.kts`
2. **Reload Gradle Projects** (automatic or manual)
3. Autocomplete works immediately

**Pro tip:** For development, you can add dependencies to `build.gradle.kts` temporarily for IDE support, then remove
them and use `@DependsOn` for portability.

---

## Best Practices

### 1. Choose the Right Method

```kotlin
// ✅ Standalone script - use @DependsOn
@file:DependsOn("com.google.code.gson:gson:2.10.1")

// ✅ Project build - use classpath (build.gradle.kts)
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. Pin Exact Versions

```kotlin
// ✅ Good - reproducible
@file:DependsOn("com.google.code.gson:gson:2.10.1")

// ❌ Bad - unpredictable
@file:DependsOn("com.google.code.gson:gson:+")
@file:DependsOn("com.google.code.gson:gson:2.10.+")
```

### 3. Document Dependencies

```kotlin
// .kite/segments/api.kite.kts
/**
 * API Client Segment
 * 
 * External dependencies:
 * - OkHttp: HTTP client
 * - Gson: JSON parsing
 */
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import okhttp3.OkHttpClient
import com.google.gson.Gson
```

### 4. Minimize Dependencies

```kotlin
// ✅ Good - only what you need
@file:DependsOn("com.google.code.gson:gson:2.10.1")

// ❌ Bad - unnecessary dependencies
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("org.apache.commons:commons-lang3:3.14.0")  // Not used
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")  // Not used
```

### 5. Cache in CI/CD

```yaml
# GitHub Actions example
- name: Cache Ivy dependencies
  uses: actions/cache@v3
  with:
    path: ~/.ivy2/cache
    key: ${{ runner.os }}-ivy-${{ hashFiles('**/*.kite.kts') }}
    
- name: Run Kite
  run: kite ride "My Ride"
```

---

## Troubleshooting

### Problem: "Unresolved reference" in IDE

**Cause:** IDE hasn't resolved dependencies yet

**Solution:**

1. Run script once: `kite ride "My Ride"`
2. Dependencies download to `~/.ivy2/cache`
3. Reload Gradle project in IntelliJ
4. Autocomplete will work

**Alternative:** Temporarily add to `build.gradle.kts` for IDE support

### Problem: Dependency resolution is slow

**Cause:** First run downloads dependencies

**Solution:**

- **First run:** Slow (downloading from Maven Central)
- **Subsequent runs:** Fast (cached in `~/.ivy2/cache`)
- This is normal! Cache the `~/.ivy2/cache` directory in CI

### Problem: "Failed to resolve dependency"

**Cause:** Wrong coordinates or version doesn't exist

**Solution:**

1. Verify on [Maven Central](https://search.maven.org/)
2. Check spelling: `group:artifact:version`
3. Ensure version exists
4. Add `@file:Repository` if using custom repo

**Example:**

```kotlin
// ❌ Wrong
@file:DependsOn("com.google.gson:gson:999.0.0")  // Version doesn't exist

// ✅ Correct
@file:DependsOn("com.google.code.gson:gson:2.10.1")  // Real version
```

### Problem: Works locally but fails in CI

**Cause:** CI can't access Maven Central or custom repository

**Solution:**

1. Ensure CI has internet access
2. Check firewall/proxy settings
3. Cache `~/.ivy2/cache` for faster builds
4. Use classpath dependencies for stricter CI control

---

## Technical Details

### How @DependsOn Works

Kite uses **Apache Ivy** for dependency resolution:

1. Parses `@file:DependsOn` and `@file:Repository` annotations
2. Creates Ivy configuration
3. Resolves dependencies from Maven repositories
4. Downloads JARs to `~/.ivy2/cache`
5. Adds to classpath automatically

**Why Ivy?**

- ✅ Java 17+ compatible
- ✅ Lightweight
- ✅ Maven-compatible (works with Maven Central)
- ✅ Handles transitive dependencies automatically

### Dependency Cache Location

- **Linux/Mac:** `~/.ivy2/cache`
- **Windows:** `C:\Users\<username>\.ivy2\cache`

---

## Summary

**Two Ways to Add Dependencies:**

1. **`@DependsOn`** - Standalone scripts, portable, no setup
2. **Classpath** - Project builds, centralized, faster

**Both approaches:**

- ✅ Full IDE support
- ✅ Work in CLI and CI/CD
- ✅ Resolve transitive dependencies
- ✅ Support Maven Central + custom repos

**Choose based on your use case:**

- **Sharing scripts?** → Use `@DependsOn`
- **Building projects?** → Use classpath
- **Quick prototype?** → Use `@DependsOn`
- **Production CI/CD?** → Use classpath

**Example:**

```kotlin
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson

segment("example") {
    execute {
        val gson = Gson()
        logger.info(gson.toJson(mapOf("status" to "works!")))
    }
}
```

It just works! 🎉

---

## Related Topics

- [Writing Segments](04-writing-segments.md) - Complete segment authoring guide
- [Execution Context](06-execution-context.md) - API reference for segment execution
- [CI Integration](11-ci-integration.md) - Using dependencies in CI/CD pipelines

---

## Next Steps

- [Integrate with CI/CD →](11-ci-integration.md)
- [Explore CLI reference →](12-cli-reference.md)
- [Troubleshooting guide →](99-troubleshooting.md)

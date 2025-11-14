# Plugin System

## Overview

Kite's plugin system allows extending functionality with reusable, modular components that can add new segment types,
helper functions, DSL extensions, and integrate with external services.

## Plugin Definition

Plugins are Kotlin libraries implementing the `KitePlugin` interface:

```kotlin
interface KitePlugin {
    val id: String
    val version: String
    val description: String?
    
    fun configure(config: Map<String, Any>) {}
    fun apply(context: PluginContext)
}

interface PluginContext {
    fun registerExtension(name: String, extension: Any)
    fun registerHelper(name: String, function: KFunction<*>)
    fun registerSegmentType(name: String, factory: (SegmentBuilder) -> Segment)
    
    val kiteVersion: String
    val workspaceDir: File
    val logger: Logger
}
```

## Plugin Discovery (MVP)

**Classpath-based** using Java Service Loader:

```kotlin
// Plugin's META-INF/services/io.kite.plugin.KitePlugin
io.kite.plugins.playstore.PlayStorePlugin
```

```kotlin
// Project's build.gradle.kts
dependencies {
    kitePlugin("io.kite.plugins:playstore:1.0.0")
}
```

## Configuration

```kotlin
// .kite/settings.kite.kts
settings {
    plugins {
        plugin("io.kite.plugins:playstore:1.0.0") {
            serviceAccountJson = env("GOOGLE_SERVICE_ACCOUNT_JSON")
            packageName = "com.example.app"
        }
        
        plugin("io.kite.plugins:slack:1.0.0") {
            webhook = env("SLACK_WEBHOOK")
            channel = "#ci-notifications"
        }
    }
}
```

## Example Plugins

### Play Store Plugin

```kotlin
// Usage in segments
segment("uploadToPlayStoreInternal") {
    execute {
        playStore {
            track = "internal"
            apk = file("app/build/outputs/apk/release/app-release.apk")
            releaseNotes {
                locale("en-US", "Bug fixes and improvements")
                locale("es-ES", "Corrección de errores y mejoras")
            }
            obfuscationMapping = file("app/build/outputs/mapping/release/mapping.txt")
        }
    }
}
```

### Translation Plugin

```kotlin
// Configuration
settings {
    plugins {
        plugin("io.kite.plugins:translations:1.0.0") {
            provider = "crowdin"
            apiKey = env("CROWDIN_API_KEY")
            projectId = env("CROWDIN_PROJECT_ID")
        }
    }
}

// Usage
segment("syncTranslations") {
    execute {
        translations {
            upload {
                source = file("app/src/main/res/values/strings.xml")
            }
            download {
                destination = file("app/src/main/res/")
                languages = listOf("es", "fr", "de", "it", "pt")
            }
        }
    }
}
```

### Docker Plugin

```kotlin
segment("buildAndPushDocker") {
    execute {
        docker {
            build {
                tag = "myapp:${context.commitSha}"
                dockerfile = "Dockerfile"
                buildArgs = mapOf("VERSION" to getCurrentVersion())
            }
            
            push {
                image = "myapp:${context.commitSha}"
                registry = "gcr.io/my-project"
            }
        }
    }
}
```

### Notification Plugin

```kotlin
segment("notifyDeployment") {
    execute {
        notifications {
            slack {
                webhook = env("SLACK_WEBHOOK")
                message = "🚀 Deployed version ${getCurrentVersion()}"
                channel = "#deployments"
            }
            
            email {
                to = listOf("team@example.com")
                subject = "Deployment Successful"
            }
        }
    }
}
```

## Creating a Plugin

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.kite:kite-plugin-api:1.0.0")
}

// Plugin implementation
class CustomServicePlugin : KitePlugin {
    override val id = "custom-service"
    override val version = "1.0.0"
    
    private lateinit var apiKey: String
    
    override fun configure(config: Map<String, Any>) {
        apiKey = config["apiKey"] as? String ?: error("apiKey required")
    }
    
    override fun apply(context: PluginContext) {
        // Register DSL extension
        context.registerExtension("customService") {
            CustomServiceDsl(apiKey)
        }
        
        // Register helper function
        context.registerHelper("uploadToService") { file: File ->
            uploadImpl(file)
        }
    }
}

// DSL Extension
class CustomServiceDsl(private val apiKey: String) {
    fun upload(file: File, metadata: Map<String, String> = emptyMap()) {
        // Implementation
    }
}
```

## Official Plugin Ecosystem (Planned)

### Core Plugins (Kite team)

- `playstore` - Google Play Store integration
- `appstore` - Apple App Store integration
- `docker` - Docker operations
- `notifications` - Slack, email, Teams
- `git` - Advanced Git operations
- `firebase` - App Distribution, Crashlytics

### Community Plugins

- `translations` - Crowdin, Lokalise, Phrase
- `sentry` - Error tracking
- `jira` - Issue tracking
- `kubernetes` - K8s deployment
- `terraform` - Infrastructure as code

## Plugin Best Practices

1. **Versioning**: Use semantic versioning
2. **Configuration**: Accept config via DSL and environment variables
3. **Error Handling**: Provide clear error messages
4. **Documentation**: Include comprehensive docs and examples
5. **Testing**: Write tests for your plugin
6. **Dependencies**: Minimize external dependencies
7. **Compatibility**: Specify compatible Kite versions

## Migration from Fastlane

Plugins can provide Fastlane-compatible APIs for easy migration:

```kotlin
segment("deployToPlayStore") {
    execute {
        supply {  // Familiar Fastlane syntax
            track = "internal"
            apk = "app/build/outputs/apk/release/app-release.apk"
        }
    }
}
```

## Future Enhancements (Phase 2)

- Plugin directory: `.kite/plugins/`
- Plugin CLI: `kite plugin install/list/update`
- Plugin registry/marketplace
- Plugin permissions and sandboxing
- Plugin templates for quick development

## Summary

- **Extensible**: Add custom functionality via plugins
- **Modular**: Reusable across projects
- **Type-safe**: Full Kotlin integration
- **Discoverable**: Auto-discovery via ServiceLoader
- **Configurable**: Settings in `settings.kite.kts`
- **Ecosystem**: Official and community plugins
- **Fastlane-compatible**: Easy migration path

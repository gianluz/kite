package io.kite.core

import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * SecretMasker masks sensitive values in logs and outputs to prevent secrets leakage.
 *
 * Thread-safe singleton that tracks registered secrets and replaces them with masked values.
 *
 * Usage:
 * ```
 * val apiKey = env("API_KEY")  // Automatically registered as secret
 * SecretMasker.registerSecret(apiKey)
 *
 * val output = exec("curl", "-H", "Authorization: Bearer $apiKey")
 * // Output will show: "Authorization: Bearer ***"
 * ```
 */
object SecretMasker {
    private val secrets = ConcurrentHashMap<String, String>()
    private const val MASK = "***"

    /**
     * Registers a value as a secret that should be masked in all outputs.
     *
     * @param value The secret value to mask (e.g., API key, token, password)
     * @param hint Optional hint to help identify which secret (e.g., "API_KEY", "DB_PASSWORD")
     */
    fun registerSecret(
        value: String,
        hint: String? = null,
    ) {
        if (value.isBlank()) return

        // Store both the value and its variations
        secrets[value] = hint ?: "SECRET"

        // Also mask URL-encoded version
        val encoded = urlEncode(value)
        if (encoded != value) {
            secrets[encoded] = hint ?: "SECRET"
        }

        // Also mask base64-encoded version (common in headers)
        val base64 = Base64.getEncoder().encodeToString(value.toByteArray())
        if (base64 != value) {
            secrets[base64] = hint?.let { "${it}_BASE64" } ?: "SECRET_BASE64"
        }
    }

    /**
     * Registers multiple secrets at once.
     */
    fun registerSecrets(values: Map<String, String>) {
        values.forEach { (hint, value) ->
            registerSecret(value, hint)
        }
    }

    /**
     * Masks all registered secrets in the given text.
     * Replaces secret values with "***" or "[SECRET_NAME:***]" if hint is available.
     *
     * @param text The text to mask
     * @param showHints If true, shows which secret was masked (e.g., "[API_KEY:***]")
     * @return The masked text
     */
    fun mask(
        text: String,
        showHints: Boolean = true,
    ): String {
        if (secrets.isEmpty()) return text

        var masked = text
        secrets.forEach { (secret, hint) ->
            if (secret.isNotEmpty()) {
                val replacement =
                    if (showHints && hint != "SECRET") {
                        "[$hint:$MASK]"
                    } else {
                        MASK
                    }
                masked = masked.replace(secret, replacement)
            }
        }
        return masked
    }

    /**
     * Clears all registered secrets.
     * Useful for testing or between ride executions.
     */
    fun clear() {
        secrets.clear()
    }

    /**
     * Returns the number of registered secrets.
     */
    fun secretCount(): Int = secrets.size

    /**
     * Checks if a value is registered as a secret.
     */
    fun isSecret(value: String): Boolean = secrets.containsKey(value)

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }
}

/**
 * Extension function to mask secrets in any string.
 */
fun String.maskSecrets(showHints: Boolean = true): String {
    return SecretMasker.mask(this, showHints)
}

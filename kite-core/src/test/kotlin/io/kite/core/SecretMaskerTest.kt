package io.kite.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretMaskerTest {
    @AfterEach
    fun cleanup() {
        SecretMasker.clear()
    }

    @Test
    fun `masks simple secret`() {
        SecretMasker.registerSecret("my-secret-key")

        val text = "The key is my-secret-key for authentication"
        val masked = SecretMasker.mask(text)

        assertEquals("The key is *** for authentication", masked)
    }

    @Test
    fun `masks secret with hint`() {
        SecretMasker.registerSecret("sk-1234567890", hint = "API_KEY")

        val text = "Using API key: sk-1234567890"
        val masked = SecretMasker.mask(text, showHints = true)

        assertEquals("Using API key: [API_KEY:***]", masked)
    }

    @Test
    fun `masks secret without hints`() {
        SecretMasker.registerSecret("sk-1234567890", hint = "API_KEY")

        val text = "Using API key: sk-1234567890"
        val masked = SecretMasker.mask(text, showHints = false)

        assertEquals("Using API key: ***", masked)
    }

    @Test
    fun `masks multiple secrets`() {
        SecretMasker.registerSecret("secret1", hint = "SECRET_1")
        SecretMasker.registerSecret("secret2", hint = "SECRET_2")

        val text = "First: secret1 and second: secret2"
        val masked = SecretMasker.mask(text, showHints = true)

        assertEquals("First: [SECRET_1:***] and second: [SECRET_2:***]", masked)
    }

    @Test
    fun `masks URL-encoded secrets`() {
        val secret = "my secret+key"
        SecretMasker.registerSecret(secret, hint = "API_KEY")

        // URL-encoded version should also be masked
        val text = "URL: https://api.example.com?key=my+secret%2Bkey"
        val masked = SecretMasker.mask(text, showHints = true)

        assertTrue(masked.contains("[API_KEY:***]"))
    }

    @Test
    fun `masks base64-encoded secrets`() {
        val secret = "mysecret"
        SecretMasker.registerSecret(secret, hint = "PASSWORD")

        // Base64 version should also be masked
        val base64 = Base64.getEncoder().encodeToString(secret.toByteArray())
        val text = "Authorization: Basic $base64"
        val masked = SecretMasker.mask(text, showHints = true)

        assertTrue(masked.contains("[PASSWORD_BASE64:***]"))
    }

    @Test
    fun `register multiple secrets at once`() {
        SecretMasker.registerSecrets(
            mapOf(
                "API_KEY" to "key123",
                "DB_PASSWORD" to "pass456",
            ),
        )

        val text = "Connecting with key123 and pass456"
        val masked = SecretMasker.mask(text, showHints = true)

        assertEquals("Connecting with [API_KEY:***] and [DB_PASSWORD:***]", masked)
    }

    @Test
    fun `masks secrets in command line`() {
        SecretMasker.registerSecret("ghp_1234567890", hint = "GITHUB_TOKEN")

        val command = "curl -H 'Authorization: token ghp_1234567890' https://api.github.com"
        val masked = SecretMasker.mask(command, showHints = true)

        assertEquals("curl -H 'Authorization: token [GITHUB_TOKEN:***]' https://api.github.com", masked)
    }

    @Test
    fun `masks secrets in JSON output`() {
        SecretMasker.registerSecret("secret-value-123", hint = "API_SECRET")

        val json = """{"api_key":"secret-value-123","name":"test"}"""
        val masked = SecretMasker.mask(json, showHints = true)

        assertEquals("""{"api_key":"[API_SECRET:***]","name":"test"}""", masked)
    }

    @Test
    fun `does not mask empty values`() {
        SecretMasker.registerSecret("")

        val text = "This should not change"
        val masked = SecretMasker.mask(text)

        assertEquals(text, masked)
    }

    @Test
    fun `tracks secret count`() {
        assertEquals(0, SecretMasker.secretCount())

        SecretMasker.registerSecret("secret1")
        // Registers: original + url-encoded (if different) + base64 (if different)
        val count1 = SecretMasker.secretCount()
        assertTrue(count1 > 0, "Should register at least one secret")

        SecretMasker.registerSecret("secret2")
        val count2 = SecretMasker.secretCount()
        assertTrue(count2 > count1, "Should register more secrets")

        SecretMasker.clear()
        assertEquals(0, SecretMasker.secretCount())
    }

    @Test
    fun `checks if value is a secret`() {
        val secret = "my-api-key"
        SecretMasker.registerSecret(secret)

        assertTrue(SecretMasker.isSecret(secret))
        assertFalse(SecretMasker.isSecret("not-a-secret"))
    }

    @Test
    fun `extension function masks secrets`() {
        SecretMasker.registerSecret("token123", hint = "TOKEN")

        val text = "Using token: token123"
        val masked = text.maskSecrets(showHints = true)

        assertEquals("Using token: [TOKEN:***]", masked)
    }

    @Test
    fun `handles multiline text`() {
        SecretMasker.registerSecret("secret-key", hint = "API_KEY")

        val text =
            """
            Line 1: public info
            Line 2: secret-key should be masked
            Line 3: more public info
            """.trimIndent()

        val masked = SecretMasker.mask(text, showHints = true)

        assertTrue(masked.contains("[API_KEY:***]"))
        assertFalse(masked.contains("secret-key"))
    }

    @Test
    fun `thread safety - concurrent registration and masking`() {
        val threads =
            (1..10).map { threadNum ->
                Thread {
                    repeat(100) {
                        SecretMasker.registerSecret("secret-$threadNum-$it", hint = "SECRET_$threadNum")
                        val text = "This contains secret-$threadNum-$it"
                        val masked = SecretMasker.mask(text)
                        assertTrue(masked.contains("***"))
                    }
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Should have registered 1000 secrets (plus their encoded versions)
        assertTrue(SecretMasker.secretCount() >= 1000)
    }
}

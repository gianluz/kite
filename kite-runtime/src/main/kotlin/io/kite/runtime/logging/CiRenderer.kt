package io.kite.runtime.logging

/**
 * Emits platform-specific collapsible CI log section markers.
 */
interface CiRenderer {
    fun sectionStart(
        name: String,
        title: String,
        collapsed: Boolean = false,
    )

    fun sectionEnd(name: String)
}

object GitLabCiRenderer : CiRenderer {
    override fun sectionStart(
        name: String,
        title: String,
        collapsed: Boolean,
    ) {
        val timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND
        val collapsedFlag = if (collapsed) "[collapsed=true]" else ""
        println("${SECTION_CONTROL_PREFIX}section_start:$timestamp:${name.toCiSectionName()}$collapsedFlag\r$SECTION_CONTROL_PREFIX$title")
    }

    override fun sectionEnd(name: String) {
        val timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND
        println("${SECTION_CONTROL_PREFIX}section_end:$timestamp:${name.toCiSectionName()}\r$SECTION_CONTROL_PREFIX")
    }

    private const val MILLIS_PER_SECOND = 1000L
    private const val SECTION_CONTROL_PREFIX = "\u001B[0K"
}

object GitHubCiRenderer : CiRenderer {
    override fun sectionStart(
        name: String,
        title: String,
        collapsed: Boolean,
    ) {
        // GitHub Actions groups must be strictly nested, but Kite segments can run in parallel.
        // Emitting group markers for overlapping segments creates misleading nested log groups,
        // so keep GitHub output flat while GitLab uses named sections that can overlap safely.
    }

    override fun sectionEnd(name: String) = Unit
}

object PlainCiRenderer : CiRenderer {
    override fun sectionStart(
        name: String,
        title: String,
        collapsed: Boolean,
    ) = Unit

    override fun sectionEnd(name: String) = Unit
}

fun resolveCiRenderer(): CiRenderer =
    when {
        System.getenv("GITLAB_CI") != null -> GitLabCiRenderer
        System.getenv("GITHUB_ACTIONS") != null -> GitHubCiRenderer
        else -> PlainCiRenderer
    }

private fun String.toCiSectionName(): String =
    replace(Regex("[^A-Za-z0-9_.-]"), "_")
        .trim('_')
        .ifEmpty { "segment" }

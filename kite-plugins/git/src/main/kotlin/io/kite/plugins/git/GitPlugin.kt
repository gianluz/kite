package io.kite.plugins.git

import io.kite.core.ExecutionContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec

/**
 * Git plugin for Kite providing type-safe Git operations.
 */
@Suppress("TooManyFunctions") // Git operations naturally require many functions
class GitPlugin(private val ctx: ExecutionContext) {
    private val repository: Repository by lazy {
        FileRepositoryBuilder()
            .setWorkTree(ctx.workspace.toFile())
            .readEnvironment()
            .findGitDir()
            .build()
    }

    private val git: Git by lazy {
        Git(repository)
    }

    /**
     * Create a Git tag.
     */
    fun tag(
        name: String,
        message: String? = null,
        force: Boolean = false,
    ) {
        require(name.isNotBlank()) { "Tag name cannot be blank" }

        ctx.logger.info("🏷️  Creating tag: $name")

        val command =
            git.tag()
                .setName(name)
                .setForceUpdate(force)

        if (message != null) {
            command.setMessage(message)
        }

        command.call()
        ctx.logger.info("✅ Tag created: $name")
    }

    /**
     * Push changes to remote repository.
     */
    fun push(
        remote: String = "origin",
        branch: String? = null,
        tags: Boolean = false,
        force: Boolean = false,
    ) {
        ctx.logger.info("⬆️  Pushing to $remote...")

        val command =
            git.push()
                .setRemote(remote)
                .setForce(force)

        if (branch != null) {
            command.setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch"))
        }

        if (tags) {
            command.setPushTags()
        }

        command.call()

        val what =
            when {
                tags && branch != null -> "branch '$branch' and tags"
                tags -> "tags"
                branch != null -> "branch '$branch'"
                else -> "current branch"
            }

        ctx.logger.info("✅ Pushed $what to $remote")
    }

    /**
     * Fetch updates from remote repository.
     *
     * @param remote Remote name (default: origin)
     * @param prune Remove deleted remote branches
     */
    fun fetch(
        remote: String = "origin",
        prune: Boolean = false,
    ) {
        ctx.logger.info("📥 Fetching from $remote...")

        git.fetch()
            .setRemote(remote)
            .setRemoveDeletedRefs(prune)
            .call()

        ctx.logger.info("✅ Fetched from $remote")
    }

    /**
     * Pull changes from remote (fetch + merge).
     *
     * @param remote Remote name (default: origin)
     * @param branch Remote branch to pull (default: current branch)
     * @param rebase Use rebase instead of merge
     */
    fun pull(
        remote: String = "origin",
        branch: String? = null,
        rebase: Boolean = false,
    ) {
        ctx.logger.info("⬇️  Pulling from $remote${if (rebase) " (with rebase)" else ""}...")

        val pullCommand =
            git.pull()
                .setRemote(remote)
                .setRebase(rebase)

        if (branch != null) {
            pullCommand.setRemoteBranchName(branch)
        }

        pullCommand.call()
        ctx.logger.info("✅ Pulled from $remote")
    }

    /**
     * Checkout branch, tag, or commit.
     *
     * @param ref Branch name, tag, or commit SHA
     * @param createBranch Create branch if it doesn't exist
     */
    fun checkout(
        ref: String,
        createBranch: Boolean = false,
    ) {
        ctx.logger.info("🔄 Checking out: $ref")

        git.checkout()
            .setName(ref)
            .setCreateBranch(createBranch)
            .call()

        ctx.logger.info("✅ Checked out: $ref")
    }

    /**
     * Merge branch into current branch.
     *
     * @param branch Branch to merge
     * @param message Merge commit message
     * @param fastForward Allow fast-forward merge
     */
    fun merge(
        branch: String,
        message: String? = null,
        fastForward: Boolean = true,
    ) {
        ctx.logger.info("🔀 Merging: $branch")

        val branchRef = repository.resolve(branch)
        if (branchRef == null) {
            ctx.logger.error("❌ Branch not found: $branch")
            error("Branch not found: $branch")
        }

        val mergeCommand = git.merge().include(branchRef)

        if (message != null) {
            mergeCommand.setMessage(message)
        }

        if (!fastForward) {
            mergeCommand.setFastForward(org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF)
        }

        val result = mergeCommand.call()

        when {
            result.mergeStatus.isSuccessful -> {
                ctx.logger.info("✅ Merged: $branch (${result.mergeStatus})")
            }

            result.conflicts != null -> {
                ctx.logger.error("❌ Merge conflicts in: ${result.conflicts.keys.joinToString()}")
                error("Merge conflicts detected")
            }

            else -> {
                ctx.logger.error("❌ Merge failed: ${result.mergeStatus}")
                error("Merge failed: ${result.mergeStatus}")
            }
        }
    }

    /**
     * Get the current branch name.
     */
    fun currentBranch(): String {
        return repository.branch ?: "HEAD"
    }

    /**
     * Check if working directory is clean.
     */
    fun isClean(): Boolean {
        val status = git.status().call()
        return status.isClean
    }

    /**
     * Get list of modified files.
     */
    fun modifiedFiles(): List<String> {
        val status = git.status().call()
        return (status.modified + status.changed).toList()
    }

    /**
     * Get list of untracked files.
     */
    fun untrackedFiles(): List<String> {
        val status = git.status().call()
        return status.untracked.toList()
    }

    /**
     * Check if a tag exists.
     */
    fun tagExists(name: String): Boolean {
        val tags = git.tagList().call()
        return tags.any { it.name == "refs/tags/$name" }
    }

    /**
     * Get the latest tag name.
     */
    fun latestTag(): String? {
        val tags = git.tagList().call()
        return tags.lastOrNull()?.name?.removePrefix("refs/tags/")
    }

    /**
     * Get the current commit SHA.
     */
    fun commitSha(short: Boolean = false): String {
        val head = repository.resolve("HEAD")
        val sha = head.name
        return if (short) sha.substring(0, 7) else sha
    }

    /**
     * Add files to staging area.
     */
    fun add(pattern: String = ".") {
        ctx.logger.info("➕ Adding: $pattern")
        git.add()
            .addFilepattern(pattern)
            .call()
    }

    /**
     * Commit changes.
     */
    fun commit(
        message: String,
        all: Boolean = false,
    ) {
        require(message.isNotBlank()) { "Commit message cannot be blank" }

        ctx.logger.info("💾 Committing: $message")

        git.commit()
            .setMessage(message)
            .setAll(all)
            .call()

        ctx.logger.info("✅ Changes committed")
    }

    /**
     * Clean up resources.
     */
    internal fun close() {
        git.close()
        repository.close()
    }
}

/**
 * Extension function to make Git plugin available in ExecutionContext.
 */
fun ExecutionContext.git(configure: GitPlugin.() -> Unit) {
    val plugin = GitPlugin(this)
    try {
        plugin.configure()
    } finally {
        plugin.close()
    }
}

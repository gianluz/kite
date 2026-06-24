package io.kite.core

/**
 * Resolves a ride flow into executable segments with synthetic dependency edges
 * that preserve sequential flow ordering.
 */
object FlowResolver {
    fun resolve(
        flow: FlowNode,
        segmentMap: Map<String, Segment>,
    ): FlowResolution {
        val context = ResolutionContext(segmentMap)
        context.resolveNode(flow)
        return FlowResolution(
            segments = context.segments.values.toList(),
            missingSegments = context.missingSegments.toSet(),
        )
    }

    private class ResolutionContext(
        private val segmentMap: Map<String, Segment>,
    ) {
        val segments = linkedMapOf<String, Segment>()
        val missingSegments = linkedSetOf<String>()

        fun resolveNode(node: FlowNode): NodeResolution =
            when (node) {
                is FlowNode.Sequential -> resolveSequential(node)
                is FlowNode.Parallel -> resolveParallel(node)
                is FlowNode.SegmentRef -> resolveSegmentRef(node)
            }

        private fun resolveSequential(node: FlowNode.Sequential): NodeResolution {
            val entrySegments = linkedSetOf<String>()
            var previousExitSegments = emptySet<String>()
            var currentExitSegments = emptySet<String>()

            for (childNode in node.nodes) {
                val child = resolveNode(childNode)
                if (child.entrySegments.isEmpty() && child.exitSegments.isEmpty()) {
                    continue
                }

                if (entrySegments.isEmpty()) {
                    entrySegments.addAll(child.entrySegments)
                }

                addDependencies(child.entrySegments, previousExitSegments)
                previousExitSegments = child.exitSegments
                currentExitSegments = child.exitSegments
            }

            return NodeResolution(
                entrySegments = entrySegments,
                exitSegments = currentExitSegments,
            )
        }

        private fun resolveParallel(node: FlowNode.Parallel): NodeResolution {
            val entrySegments = linkedSetOf<String>()
            val exitSegments = linkedSetOf<String>()

            for (childNode in node.nodes) {
                val child = resolveNode(childNode)
                entrySegments.addAll(child.entrySegments)
                exitSegments.addAll(child.exitSegments)
            }

            return NodeResolution(
                entrySegments = entrySegments,
                exitSegments = exitSegments,
            )
        }

        private fun resolveSegmentRef(node: FlowNode.SegmentRef): NodeResolution {
            val segment = segmentMap[node.segmentName]
            if (segment == null) {
                missingSegments.add(node.segmentName)
                return NodeResolution.EMPTY
            }

            val finalSegment = applyOverrides(segment, node.overrides)
            segments[finalSegment.name] = finalSegment

            return NodeResolution(
                entrySegments = setOf(finalSegment.name),
                exitSegments = setOf(finalSegment.name),
            )
        }

        private fun addDependencies(
            segmentNames: Set<String>,
            dependencies: Set<String>,
        ) {
            if (dependencies.isEmpty()) return

            for (segmentName in segmentNames) {
                val segment = segments[segmentName] ?: continue
                val combinedDeps = (segment.dependsOn + dependencies).distinct()
                segments[segmentName] = segment.copy(dependsOn = combinedDeps)
            }
        }

        private fun applyOverrides(
            segment: Segment,
            overrides: SegmentOverrides,
        ): Segment {
            var result = segment

            overrides.dependsOn?.let { overrideDeps ->
                val combinedDeps = (segment.dependsOn + overrideDeps).distinct()
                result = result.copy(dependsOn = combinedDeps)
            }

            overrides.condition?.let { overrideCondition ->
                result = result.copy(condition = overrideCondition)
            }

            overrides.timeout?.let { overrideTimeout ->
                result = result.copy(timeout = overrideTimeout)
            }

            if (!overrides.enabled) {
                result = result.copy(condition = { false })
            }

            return result
        }
    }

    private data class NodeResolution(
        val entrySegments: Set<String>,
        val exitSegments: Set<String>,
    ) {
        companion object {
            val EMPTY = NodeResolution(emptySet(), emptySet())
        }
    }
}

data class FlowResolution(
    val segments: List<Segment>,
    val missingSegments: Set<String>,
)

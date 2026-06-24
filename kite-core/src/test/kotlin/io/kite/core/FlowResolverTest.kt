package io.kite.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FlowResolverTest {
    @Test
    fun `sequential flow creates dependency chain`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("test"),
                            FlowNode.SegmentRef("deploy"),
                        ),
                    ),
                segmentMap = segmentMap("build", "test", "deploy"),
            )

        assertEquals(emptySet(), resolution.missingSegments)
        assertEquals(emptyList(), resolution.segment("build").dependsOn)
        assertEquals(listOf("build"), resolution.segment("test").dependsOn)
        assertEquals(listOf("test"), resolution.segment("deploy").dependsOn)
    }

    @Test
    fun `sequential flow preserves explicit dependencies`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("test", SegmentOverrides(dependsOn = listOf("lint"))),
                        ),
                    ),
                segmentMap = segmentMap("build", "test", "lint"),
            )

        assertEquals(listOf("lint", "build"), resolution.segment("test").dependsOn)
    }

    @Test
    fun `parallel block after segment depends on previous segment`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.Parallel(
                                listOf(
                                    FlowNode.SegmentRef("unitTest"),
                                    FlowNode.SegmentRef("lint"),
                                ),
                            ),
                        ),
                    ),
                segmentMap = segmentMap("build", "unitTest", "lint"),
            )

        assertEquals(listOf("build"), resolution.segment("unitTest").dependsOn)
        assertEquals(listOf("build"), resolution.segment("lint").dependsOn)
    }

    @Test
    fun `segment after parallel block depends on all previous parallel segments`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.Parallel(
                                listOf(
                                    FlowNode.SegmentRef("unitTest"),
                                    FlowNode.SegmentRef("lint"),
                                ),
                            ),
                            FlowNode.SegmentRef("deploy"),
                        ),
                    ),
                segmentMap = segmentMap("build", "unitTest", "lint", "deploy"),
            )

        assertEquals(listOf("unitTest", "lint"), resolution.segment("deploy").dependsOn)
    }

    @Test
    fun `nested sequential block inside parallel block keeps local ordering only`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Parallel(
                        listOf(
                            FlowNode.Sequential(
                                listOf(
                                    FlowNode.SegmentRef("buildApi"),
                                    FlowNode.SegmentRef("testApi"),
                                ),
                            ),
                            FlowNode.Sequential(
                                listOf(
                                    FlowNode.SegmentRef("buildWeb"),
                                    FlowNode.SegmentRef("testWeb"),
                                ),
                            ),
                        ),
                    ),
                segmentMap = segmentMap("buildApi", "testApi", "buildWeb", "testWeb"),
            )

        assertEquals(listOf("buildApi"), resolution.segment("testApi").dependsOn)
        assertEquals(listOf("buildWeb"), resolution.segment("testWeb").dependsOn)
        assertEquals(emptyList(), resolution.segment("buildApi").dependsOn)
        assertEquals(emptyList(), resolution.segment("buildWeb").dependsOn)
    }

    @Test
    fun `missing segment is not used as synthetic dependency`() {
        val resolution =
            FlowResolver.resolve(
                flow =
                    FlowNode.Sequential(
                        listOf(
                            FlowNode.SegmentRef("build"),
                            FlowNode.SegmentRef("missing"),
                            FlowNode.SegmentRef("deploy"),
                        ),
                    ),
                segmentMap = segmentMap("build", "deploy"),
            )

        assertEquals(setOf("missing"), resolution.missingSegments)
        assertEquals(listOf("build"), resolution.segment("deploy").dependsOn)
    }

    private fun FlowResolution.segment(name: String): Segment = segments.first { it.name == name }

    private fun segmentMap(vararg names: String): Map<String, Segment> = names.associateWith { name -> Segment(name = name, execute = {}) }
}

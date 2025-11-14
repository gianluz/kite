# Overview & Problem Statement

## What is Kite?

Kite is a Kotlin-based CI ride runner designed to replace Fastlane and improve CI/CD workflows by providing a
programmatic, type-safe, and testable way to define build rides. It addresses the limitations of bash scripting in
CI configuration files while enabling better code reuse, debugging, and local testing.

**Key Design Principle**: Modular segment definitions with composable configurations for different ride scenarios.

## Problem Statement

### Current Pain Points

1. **Inefficient Stage Separation**: In GitLab CI (and similar platforms), splitting build and test stages often results
   in redundant rebuilds

2. **Limited Bash Scripting**: CI YAML files rely on bash scripts that are:
    - Hard to test and debug
    - Lack type safety
    - Difficult to reuse across projects
    - Not easily testable with unit tests

3. **Fastlane Limitations**: Ruby-based Fastlane scripts are:
    - Slower than compiled languages
    - Limited ecosystem for mobile/backend development
    - Additional dependency to manage

4. **No Logical Parallelization**: Cannot parallelize segments within a single CI stage/container

### Kite's Solution

- **Single Stage, Multiple Segments**: Run multiple logical steps (build, test, integration test) within one CI
  stage/container

- **Kotlin DSL**: Type-safe, IDE-friendly segment definitions

- **Modular Segment Library**: Define segments once, reuse across multiple configurations

- **Ride Composition**: Different ride configs for different scenarios (ordinary MR, release MR, etc.)

- **Internal Parallelization**: Simulate parallel stages within a single Docker container

- **Testable Infrastructure**: Unit test your CI/CD scripts

- **Conditional Execution**: Smart segment execution based on context (MR type, branch, environment)

- **Artifact Management**: Efficient sharing of build outputs between segments

## Core Value Propositions

1. **Developer Experience**: Write CI logic in Kotlin with full IDE support (autocomplete, refactoring, type checking)

2. **Debuggability**: Run and debug rides locally before pushing to CI

3. **Performance**: Avoid redundant rebuilds by keeping everything in one container/stage

4. **Modularity**: Define segments once, compose them into different rides

5. **Testability**: Unit test your CI/CD infrastructure code

6. **Parallelization**: Run independent segments in parallel within the same CI stage

## Key Terminology

- **Ride**: A pipeline/workflow composed of multiple segments (e.g., "MR Ride", "Release Ride")
- **Segment**: A single unit of work within a ride (e.g., "build", "test", "deploy")
- **Flow**: The execution order and dependencies of segments within a ride
- **Configuration**: A `.kite.kts` file that defines a ride's segments and execution flow
- **Artifact**: Output from a segment that can be consumed by other segments
- **Context**: Runtime information (branch, commit, CI platform, etc.) available to segments

## Success Metrics

- **Developer Experience**: Reduce ride definition time by 50%
- **Execution Speed**: No more than 5% overhead compared to raw bash
- **Debuggability**: Ability to run and debug rides locally before CI
- **Adoption**: Replace Fastlane in at least one production project
- **Testability**: Enable unit testing of 80%+ of CI logic
- **Modularity**: Segments defined once, reused across 3+ different rides

# TODO

## Documentation Migration (In Progress)

### ✅ Completed

- [x] Terminology update: "pipeline" → "ride", "task" → "segment"
- [x] File extension unification: All files use `.kite.kts`
- [x] Modular spec structure created in `specs/` directory
- [x] specs/01-overview.md - Problem statement and value propositions
- [x] specs/02-core-concepts.md - Rides, segments, flows
- [x] specs/03-dsl-configuration.md - Complete DSL reference
- [x] specs/04-execution-model.md - Execution engine and scheduling
- [x] README.md updated with new terminology
- [x] CHANGELOG.md updated with changes

### 🚧 In Progress

- [ ] Extract remaining sections from SPECS.md into modular docs:
    - [ ] specs/05-parallelization.md - Extract parallelization deep dive
    - [ ] specs/06-builtin-features.md - Extract built-in helpers and features
    - [ ] specs/07-plugin-system.md - Extract plugin system specification
    - [ ] specs/08-platform-integration.md - Extract CI/CD platform integration
    - [ ] specs/09-architecture.md - Extract technical architecture

- [ ] Archive or remove old SPECS.md once migration is complete
- [ ] Update DEVELOPMENT_PLAN.md with new terminology (ride/segment)

## Development Tasks (Next Steps)

### Epic 1.2: Core Domain Models

- [ ] Task 1.2.1: Define Segment model with execution state
- [ ] Task 1.2.2: Define ExecutionContext model
- [ ] Task 1.2.3: Define RideConfiguration model
- [ ] Task 1.2.4: Define Platform Adapters interface

### Epic 1.3: Kotlin Scripting Integration

- [ ] Task 1.3.1: Set up Kotlin scripting engine
- [ ] Task 1.3.2: Implement .kite.kts file compilation
- [ ] Task 1.3.3: Create DSL builder classes
- [ ] Task 1.3.4: Implement script discovery and loading

## Notes

- Keep SPECS.md in root temporarily as reference
- All new development should use: ride, segment, .kite.kts terminology
- CLI commands: `kite ride`, `kite segments`, `kite run`
- File structure: `.kite/segments/`, `.kite/rides/`, `.kite/settings.kite.kts`

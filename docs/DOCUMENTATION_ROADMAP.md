# Documentation Refactoring Roadmap

This document outlines the plan to reorganize Kite's documentation for public release.

## Goals

1. **User-facing** - Focus on helping users get started and use Kite effectively
2. **Well-organized** - Numbered structure for easy navigation
3. **Web-ready** - Can be easily deployed to docs site
4. **Searchable** - Clear hierarchy and naming
5. **Professional** - Production-quality documentation

## New Structure

```
docs/
├── 00-index.md                    # Documentation home ✅ CREATED
├── 01-getting-started.md          # Quick start guide
├── 02-installation.md             # Installation methods
├── 03-core-concepts.md            # Rides, Segments, Flows
├── 04-writing-segments.md         # Creating segments
├── 05-writing-rides.md            # Creating rides
├── 06-execution-context.md        # Using context APIs
├── 07-parallel-execution.md       # Parallel flows
├── 08-artifacts.md                # Artifact management
├── 09-secrets.md                  # Secret management
├── 10-external-dependencies.md    # @DependsOn usage
├── 11-ci-integration.md           # GitHub Actions, GitLab CI
├── 12-cli-reference.md            # Command-line usage
├── 99-troubleshooting.md          # Common issues
├── api/                           # API documentation
│   ├── 00-index.md               # API overview
│   ├── dsl.md                    # DSL API reference
│   ├── core.md                   # Core classes
│   └── runtime.md                # Runtime APIs
├── dev/                          # Developer documentation
│   ├── contributing.md           # Contribution guidelines ✅ MOVED
│   ├── code-quality.md           # Quality standards ✅ MOVED
│   ├── architecture.md           # System architecture
│   └── testing.md                # Testing strategy
└── archive/                      # Old internal docs
```

## Migration Plan

### Phase 1: Structure Setup ✅ DONE

- [x] Create directory structure
- [x] Create main index (00-index.md)
- [x] Move developer docs to docs/dev/
- [x] Create reorganization script

### Phase 2: User-Facing Content 🔄 IN PROGRESS

Source mapping from existing docs:

| New File | Source | Status |
|----------|--------|--------|
| 01-getting-started.md | EXTERNAL_PROJECT_SETUP.md | ⏳ TODO |
| 02-installation.md | EXTERNAL_PROJECT_SETUP.md | ⏳ TODO |
| 03-core-concepts.md | [NEW] + specs/ | ⏳ TODO |
| 04-writing-segments.md | [NEW] | ⏳ TODO |
| 05-writing-rides.md | [NEW] | ⏳ TODO |
| 06-execution-context.md | [NEW] | ⏳ TODO |
| 07-parallel-execution.md | [NEW] | ⏳ TODO |
| 08-artifacts.md | ARTIFACTS*.md | ⏳ TODO |
| 09-secrets.md | SECURITY.md | ⏳ TODO |
| 10-external-dependencies.md | EXTERNAL_DEPENDENCIES.md | ⏳ TODO |
| 11-ci-integration.md | CI_INTEGRATION.md + GITHUB_ACTIONS.md | ⏳ TODO |
| 12-cli-reference.md | [NEW] | ⏳ TODO |
| 99-troubleshooting.md | IDE_*.md | ⏳ TODO |

### Phase 3: API Documentation ⏳ TODO

Generate from KDoc comments in source code:

- [ ] api/00-index.md - Overview of all APIs
- [ ] api/dsl.md - DSL builder functions (RideDsl, SegmentDsl)
- [ ] api/core.md - Core classes (Ride, Segment, ExecutionContext, etc.)
- [ ] api/runtime.md - Runtime APIs (Scheduler, ExecutionEngine, etc.)

**Tool**: Use Dokka or custom generator

### Phase 4: Developer Documentation ⏳ TODO

- [x] Move CONTRIBUTING.md → dev/contributing.md
- [x] Move CODE_QUALITY.md → dev/code-quality.md
- [ ] Create dev/architecture.md (system design, module structure)
- [ ] Consolidate INTEGRATION_TESTING_STRATEGY.md → dev/testing.md

### Phase 5: Archive ⏳ TODO

Move internal/outdated docs to archive/:

- [ ] DOCUMENTATION_CENSUS.md
- [ ] LIFECYCLE_HOOKS.md
- [ ] README.md (old docs readme)
- [ ] INTELLIJ_SCRIPT_CONFIGURATION.md (empty file)

### Phase 6: Polish & Review ⏳ TODO

- [ ] Update all internal links
- [ ] Add navigation links between pages
- [ ] Verify code examples work
- [ ] Proofread all content
- [ ] Add screenshots/diagrams where helpful
- [ ] Update main README.md to link to new structure

## Naming Convention

**Pattern**: `NN-page-name.md`

- `NN` = Two-digit number (00-99)
- `page-name` = Kebab-case descriptive name
- Numbers define logical order
- Special numbers:
    - `00` = Index/Overview pages
    - `01-12` = Core user documentation
    - `99` = Troubleshooting/FAQ

## Content Guidelines

### User-Facing Docs

- **Audience**: Developers using Kite
- **Tone**: Friendly, instructive, example-driven
- **Format**:
    - Clear headings
    - Code examples with explanations
    - Real-world use cases
    - Links to related topics

### API Documentation

- **Audience**: Developers integrating with Kite programmatically
- **Tone**: Technical, precise, reference-style
- **Format**:
    - Function signatures
    - Parameter descriptions
    - Return value documentation
    - Usage examples

### Developer Documentation

- **Audience**: Contributors to Kite
- **Tone**: Technical, detailed, educational
- **Format**:
    - Architecture diagrams
    - Design decisions
    - Testing strategies
    - Contribution workflow

## Tools & Scripts

- `scripts/reorganize-docs.sh` - Shows migration plan
- Dokka - Generate API docs from KDoc
- [Future] `scripts/validate-docs.sh` - Check links, examples
- [Future] MkDocs/Docusaurus for web deployment

## Timeline

- **Week 1**: Structure setup + move existing content ← WE ARE HERE
- **Week 2**: Write new user-facing guides
- **Week 3**: Generate API documentation
- **Week 4**: Polish, review, and deploy

## Notes

- Keep old docs until migration is complete
- Test all code examples before publishing
- Get feedback from beta users
- Consider adding video tutorials later
- Plan for localization (future)

## Success Criteria

- ✅ All user guides complete and tested
- ✅ API documentation generated and accurate
- ✅ No broken links
- ✅ Professional appearance
- ✅ Easy to navigate
- ✅ Deployable to docs site

---

**Status**: 🔄 Phase 1 Complete, Phase 2 In Progress
**Last Updated**: 2025-11-24

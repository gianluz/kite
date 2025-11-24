#!/bin/bash
#
# Script to reorganize documentation into the new structure
#
# This script helps map old docs to new numbered structure

set -e

echo "📚 Documentation Reorganization Plan"
echo "════════════════════════════════════════════════════════════"
echo ""

cat << 'EOF'
Current Docs → New Structure:
══════════════════════════════════════════════════════════════

User-Facing Docs (docs/):
────────────────────────────────────────────────────────────
EXTERNAL_PROJECT_SETUP.md      → 01-getting-started.md
                                → 02-installation.md
[NEW]                           → 03-core-concepts.md
[NEW]                           → 04-writing-segments.md
[NEW]                           → 05-writing-rides.md
[NEW]                           → 06-execution-context.md
[NEW]                           → 07-parallel-execution.md
ARTIFACTS*.md                   → 08-artifacts.md
SECURITY.md                     → 09-secrets.md
EXTERNAL_DEPENDENCIES.md        → 10-external-dependencies.md
CI_INTEGRATION.md + GITHUB_ACTIONS.md → 11-ci-integration.md
[NEW]                           → 12-cli-reference.md
IDE_*.md                        → 99-troubleshooting.md

API Documentation (docs/api/):
────────────────────────────────────────────────────────────
[Generate from KDoc]            → 00-index.md
[Generate from KDoc]            → dsl.md
[Generate from KDoc]            → core.md
[Generate from KDoc]            → runtime.md

Developer Docs (docs/dev/):
────────────────────────────────────────────────────────────
CONTRIBUTING.md                 → contributing.md
CODE_QUALITY.md                 → code-quality.md
[NEW]                           → architecture.md
INTEGRATION_TESTING_STRATEGY.md → testing.md

To Archive (docs/archive/):
────────────────────────────────────────────────────────────
DOCUMENTATION_CENSUS.md         → archive/
LIFECYCLE_HOOKS.md              → archive/
README.md                       → archive/
INTELLIJ_SCRIPT_CONFIGURATION.md → archive/

EOF

echo ""
echo "Next Steps:"
echo "1. Review the plan above"
echo "2. Create new content for [NEW] pages"
echo "3. Consolidate and refactor existing content"
echo "4. Move developer docs to docs/dev/"
echo "5. Create API docs from KDoc comments"
echo "6. Archive old internal docs"
echo ""

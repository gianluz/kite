#!/bin/bash
#
# Install Git hooks for Kite development
#
# This script symlinks the hooks from scripts/git-hooks/ to .git/hooks/
# so they stay up-to-date with the repository.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR/git-hooks"
GIT_HOOKS_DIR="$SCRIPT_DIR/../.git/hooks"

echo "🪝 Installing Git hooks..."

# Check if we're in a git repository
if [ ! -d "$GIT_HOOKS_DIR" ]; then
    echo "❌ Error: .git/hooks directory not found"
    echo "   Make sure you're running this from the repository root"
    exit 1
fi

# Install each hook
for hook in "$HOOKS_DIR"/*; do
    if [ -f "$hook" ]; then
        hook_name=$(basename "$hook")
        target="$GIT_HOOKS_DIR/$hook_name"
        
        # Remove existing hook (symlink or file)
        if [ -e "$target" ] || [ -L "$target" ]; then
            rm "$target"
        fi
        
        # Create symlink
        ln -s "../../scripts/git-hooks/$hook_name" "$target"
        chmod +x "$hook"
        
        echo "  ✅ Installed: $hook_name"
    fi
done

echo ""
echo "✨ Git hooks installed successfully!"
echo ""
echo "Installed hooks:"
echo "  • pre-push: Runs quality checks (ktlint + detekt) before pushing"
echo ""
echo "To bypass a hook temporarily:"
echo "  git push --no-verify"
echo ""

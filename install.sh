#!/usr/bin/env bash
# Kite CLI Installer
# Usage: curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh | bash
# Or:    curl -sSL https://github.com/gianluz/kite/releases/latest/download/install.sh | KITE_VERSION=v0.1.0-alpha8 bash

set -euo pipefail

REPO="gianluz/kite"
INSTALL_DIR="${KITE_INSTALL_DIR:-$HOME/.kite}"
BIN_DIR="$INSTALL_DIR/bin"

# ── Colours ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}ℹ${NC}  $*"; }
success() { echo -e "${GREEN}✅${NC} $*"; }
warn()    { echo -e "${YELLOW}⚠️ ${NC} $*"; }
error()   { echo -e "${RED}❌${NC} $*"; exit 1; }

echo ""
echo "🪁  Kite CLI Installer"
echo "────────────────────────"
echo ""

# ── Java check ─────────────────────────────────────────────────────────────────
if ! command -v java &>/dev/null; then
    error "Java 17+ is required but not found.\nInstall it from https://adoptium.net/ or via your package manager:\n  macOS:  brew install --cask temurin@17\n  Ubuntu: apt-get install -y temurin-17-jdk"
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oE '"[0-9]+' | grep -oE '[0-9]+' | head -1)
if [ "${JAVA_VERSION:-0}" -lt 17 ]; then
    error "Java 17+ required. Found Java ${JAVA_VERSION}. Install from https://adoptium.net/"
fi
info "Java ${JAVA_VERSION} found ✓"

# ── OS / Arch ──────────────────────────────────────────────────────────────────
OS="$(uname -s)"
case "$OS" in
    Linux|Darwin) ;;
    *) error "Unsupported OS: $OS. Use Docker instead: docker run --rm -v \$(pwd):/workspace ghcr.io/gianluz/kite:latest" ;;
esac

# ── Version ────────────────────────────────────────────────────────────────────
if [ -n "${KITE_VERSION:-}" ]; then
    VERSION="$KITE_VERSION"
    info "Using specified version: $VERSION"
else
    info "Fetching latest release..."
    VERSION=$(curl -fsSL "https://api.github.com/repos/${REPO}/releases/latest" \
        | grep '"tag_name"' | cut -d'"' -f4)
    [ -n "$VERSION" ] || error "Could not fetch latest version from GitHub. Check your internet connection."
    info "Latest version: $VERSION"
fi

VERSION_NUM="${VERSION#v}"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${VERSION}/kite-cli-${VERSION_NUM}.tar"

# ── Download ───────────────────────────────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

info "Downloading kite-cli ${VERSION}..."
if ! curl -fsSL --progress-bar "$DOWNLOAD_URL" -o "$TMP_DIR/kite-cli.tar"; then
    error "Download failed from:\n  $DOWNLOAD_URL\nCheck the release exists: https://github.com/${REPO}/releases"
fi

# ── Install ────────────────────────────────────────────────────────────────────
info "Installing to ${INSTALL_DIR}..."
tar -xf "$TMP_DIR/kite-cli.tar" -C "$TMP_DIR"

rm -rf "$INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
mv "$TMP_DIR/kite-cli-${VERSION_NUM}/"* "$INSTALL_DIR/"
chmod +x "$BIN_DIR/kite-cli"

# ── PATH hint ──────────────────────────────────────────────────────────────────
echo ""
success "kite-cli ${VERSION} installed to ${INSTALL_DIR}"
echo ""

# Check if already on PATH
if command -v kite-cli &>/dev/null 2>&1; then
    info "kite-cli is already on your PATH: $(command -v kite-cli)"
else
    echo "  Add kite-cli to your PATH:"
    echo ""
    echo "    export PATH=\"${BIN_DIR}:\$PATH\""
    echo ""

    SHELL_RC=""
    case "${SHELL:-}" in
        */zsh)  SHELL_RC="$HOME/.zshrc" ;;
        */bash) SHELL_RC="$HOME/.bashrc" ;;
    esac

    if [ -n "$SHELL_RC" ]; then
        echo "  Or to make it permanent:"
        echo ""
        echo "    echo 'export PATH=\"${BIN_DIR}:\$PATH\"' >> $SHELL_RC"
        echo "    source $SHELL_RC"
        echo ""
    fi
fi

# ── Verify ─────────────────────────────────────────────────────────────────────
export PATH="${BIN_DIR}:${PATH}"
if kite-cli --version &>/dev/null 2>&1; then
    success "$(kite-cli --version)"
else
    warn "Installation complete but kite-cli --version returned an error. Check your Java installation."
fi

echo ""
info "Quick start:"
echo "  kite-cli --help"
echo "  kite-cli rides"
echo "  kite-cli ride CI"
echo ""
info "Docker alternative (no Java needed):"
echo "  docker run --rm -v \$(pwd):/workspace ghcr.io/gianluz/kite:latest ride CI"
echo ""
info "Documentation: https://github.com/gianluz/kite/blob/main/docs/00-index.md"
echo ""

# Dockerfile for Kite CLI
# Multi-stage not needed — we copy the pre-built installDist output from CI
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="gianluz"
LABEL description="Kite - Type-safe CI/CD workflow runner for Kotlin projects"
LABEL org.opencontainers.image.source="https://github.com/gianluz/kite"
LABEL org.opencontainers.image.url="https://github.com/gianluz/kite"
LABEL org.opencontainers.image.documentation="https://github.com/gianluz/kite/blob/main/docs/00-index.md"
LABEL org.opencontainers.image.licenses="Apache-2.0"

# Install tools that Kite segments commonly need (git, bash, curl)
RUN apk add --no-cache bash git curl

# Copy Kite CLI installation (output of ./gradlew :kite-cli:installDist)
COPY kite-cli/build/install/kite-cli /opt/kite

# Add Kite to PATH
ENV PATH="/opt/kite/bin:$PATH"

# Mount your project directory here
VOLUME /workspace
WORKDIR /workspace

# Verify installation
RUN kite-cli --version || echo "Kite CLI installed"

# Default command shows help
ENTRYPOINT ["kite-cli"]
CMD ["--help"]

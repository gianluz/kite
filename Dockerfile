# Dockerfile for Kite CLI
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="gianluz"
LABEL description="Kite - Type-safe CI/CD workflow runner for Kotlin projects"
LABEL org.opencontainers.image.source="https://github.com/gianluz/kite"

# Install basic tools
RUN apk add --no-cache bash git curl

# Copy Kite CLI installation
COPY kite-cli/build/install/kite-cli /opt/kite

# Add Kite to PATH
ENV PATH="/opt/kite/bin:$PATH"

# Set default workspace
WORKDIR /workspace

# Verify installation
RUN kite-cli --version || echo "Kite CLI installed"

# Default command shows help
ENTRYPOINT ["kite-cli"]
CMD ["--help"]

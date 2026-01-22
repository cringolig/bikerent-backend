# ==============================================
# BikeRent Backend Dockerfile
# Multi-stage build for security and efficiency
# ==============================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy POM first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================
# Stage 2: Runtime
# ==============================================
FROM tomcat:10.1-jdk17-temurin

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Create non-root user
RUN groupadd -r bikerent && useradd -r -g bikerent bikerent

# Copy WAR file
COPY --from=builder /build/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Set permissions
RUN chown -R bikerent:bikerent /usr/local/tomcat/webapps \
    && chown -R bikerent:bikerent /usr/local/tomcat/logs \
    && chown -R bikerent:bikerent /usr/local/tomcat/work \
    && chown -R bikerent:bikerent /usr/local/tomcat/temp \
    && mkdir -p /usr/local/tomcat/conf/Catalina/localhost \
    && chown -R bikerent:bikerent /usr/local/tomcat/conf/Catalina

# Security: Run as non-root user
USER bikerent

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Start Tomcat
CMD ["catalina.sh", "run"]

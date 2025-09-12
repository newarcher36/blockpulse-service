# Use Temurin JDK 21 on Alpine
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app
RUN apk add --no-cache curl

# Add metadata
LABEL maintainer="bitcoin-fee-analyzer"
LABEL description="BTC fee analyzer"

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean install -DskipTests

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/target/blockpulse-service.jar"]
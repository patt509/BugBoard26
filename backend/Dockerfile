# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM payara/server-full:6.2024.1-jdk17

# Copy the WAR file from builder
COPY --from=builder /app/target/bugboard-backend.war /opt/payara/appserver/glassfish/domains/domain1/autodeploy/bugboard.war

# Expose ports
EXPOSE 8080 4848

# Start Payara Server
CMD ["asadmin", "start-domain", "--verbose"]

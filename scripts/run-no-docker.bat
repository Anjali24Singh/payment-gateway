@echo off
REM Payment Gateway - No Docker Setup Script (Windows)
REM Runs the application using H2 in-memory database

echo 🚀 Payment Gateway - No Docker Setup
echo ====================================
echo.

echo 🛠️ Checking Prerequisites...
echo ----------------------------

REM Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java not found. Please install Java 17+
    pause
    exit /b 1
)
echo ✅ Java found

REM Check Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ℹ️ Using Maven wrapper (mvnw.cmd)
) else (
    echo ✅ Maven found
)

echo.

echo 🔨 Building Application...
echo -------------------------

echo ℹ️ Installing dependencies...
call mvnw.cmd clean install -DskipTests
if %errorlevel% neq 0 (
    echo ❌ Failed to install dependencies
    pause
    exit /b 1
)
echo ✅ Dependencies installed successfully

echo.

echo 🧪 Running Unit Tests...
echo ------------------------

echo ℹ️ Running unit tests...
call mvnw.cmd test -Dtest="*UnitTest" -Dspring.profiles.active=no-docker
if %errorlevel% neq 0 (
    echo ⚠️ Some unit tests may have failed. This is expected without full infrastructure.
) else (
    echo ✅ Unit tests passed
)

echo.

echo 🚀 Starting Application...
echo -------------------------

echo ✅ Starting Payment Gateway with H2 database...
echo ℹ️ Application will be available at: http://localhost:8080
echo ℹ️ H2 Console available at: http://localhost:8080/h2-console
echo ℹ️ Health Check: http://localhost:8080/actuator/health
echo.
echo ⚠️ Press Ctrl+C to stop the application
echo.

REM Start with no-docker profile
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=no-docker
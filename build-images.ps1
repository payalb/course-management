# Build all Docker images for Kubernetes
Write-Host "Building Docker images..." -ForegroundColor Green

# Step 1: Build base images with parent POMs (only need to rebuild when parent POMs change)
Write-Host "Building base images with parent POMs..." -ForegroundColor Cyan
docker build -t bi-parent-base:latest -f bi-parent/Dockerfile .
docker build -t bi-parent-rdbms-base:latest -f bi-parent-rdbms/Dockerfile .

# Step 2: Build service images (fast - use pre-built parent POMs)
Write-Host "`nBuilding service images..." -ForegroundColor Cyan

# Build auth service
Write-Host "Building bi-auth-service..." -ForegroundColor Yellow
docker build -t bi-auth-service:latest -f bi-auth-service/Dockerfile .

# Build API Gateway
Write-Host "Building bi-api-gateway-service..." -ForegroundColor Yellow
docker build -t bi-api-gateway-service:latest -f bi-api-gateway-service/Dockerfile .

# Build course command service
Write-Host "Building bi-course-command-service..." -ForegroundColor Yellow
docker build -t bi-course-command-service:latest -f bi-course-command-service/Dockerfile .

# Build course query service
Write-Host "Building bi-course-query-service..." -ForegroundColor Yellow
docker build -t bi-course-query-service:latest -f bi-course-query-service/Dockerfile .

# Build enrollment service
Write-Host "Building bi-enrollment-service..." -ForegroundColor Yellow
docker build -t bi-enrollment-service:latest -f bi-enrollment-service/Dockerfile .

# Build React UI
Write-Host "Building bi-course-react..." -ForegroundColor Yellow
docker build -t bi-course-react:latest -f course-react/Dockerfile course-react/

Write-Host "All images built successfully!" -ForegroundColor Green
Write-Host "Listing images:" -ForegroundColor Cyan
docker images | Select-String "bi-"

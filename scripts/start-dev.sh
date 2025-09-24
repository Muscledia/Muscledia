#!/bin/bash

echo "🚀 Starting Muscledia Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Create network if it doesn't exist
docker network create muscledia-network 2>/dev/null || true

# Pull latest images
echo "📦 Pulling latest base images..."
docker-compose pull

# Build and start services
echo "🔨 Building and starting services..."
docker-compose up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 30

# Check service health
echo "🏥 Checking service health..."
services=("mysql" "mongodb" "redis" "service-discovery" "api-gateway" "user-service" "workout-service" "gamification-service")

for service in "${services[@]}"; do
    if docker-compose ps | grep -q "$service.*healthy\|$service.*Up"; then
        echo "✅ $service is healthy"
    else
        echo "❌ $service is not healthy"
        docker-compose logs "$service" --tail=20
    fi
done

echo ""
echo "🎉 Development environment started!"
echo "📊 Service Discovery UI: http://localhost:8761"
echo "🌐 API Gateway: http://localhost:8080"
echo "📝 API Documentation:"
echo "   - User Service: http://localhost:8081/swagger-ui.html"
echo "   - Workout Service: http://localhost:8082/swagger-ui.html"
echo "   - Gamification Service: http://localhost:8083/swagger-ui.html"
echo ""
echo "🔍 To view logs: docker-compose logs -f [service-name]"
echo "🛑 To stop: docker-compose down"
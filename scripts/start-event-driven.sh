#!/bin/bash
# scripts/start-event-driven.sh

echo "🚀 Starting Muscledia Event-Driven Platform..."

# Step 1: Start infrastructure services
echo "🔧 Starting infrastructure services..."
docker-compose up -d zookeeper kafka mongodb mysql redis

# Step 2: Wait for services to be ready
echo "⏳ Waiting for infrastructure to be ready..."
sleep 60

# Step 3: Create Kafka topics
echo "📋 Creating Kafka topics..."
./scripts/create-kafka-topics.sh

# Step 4: Start application services
echo "🚀 Starting application services..."
docker-compose up -d service-discovery api-gateway user-service workout-service gamification-service

# Step 5: Start monitoring
echo "📊 Starting monitoring services..."
docker-compose up -d kafka-ui nginx

echo ""
echo "✅ Event-driven platform started successfully!"
echo ""
echo "🌐 Access Points:"
echo "   API Gateway: http://localhost:8080"
echo "   Kafka UI: http://localhost:8090"
echo "   Eureka: http://localhost:8761"
echo "   User Service: http://localhost:8081/swagger-ui.html"
echo "   Workout Service: http://localhost:8082/swagger-ui.html"
echo "   Gamification Service: http://localhost:8083/swagger-ui.html"
echo ""
echo "📋 Kafka Topics:"
docker exec muscledia-kafka kafka-topics --list --bootstrap-server localhost:9092
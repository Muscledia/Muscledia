#!/bin/bash
# scripts/verify-mongodb.sh

echo "🔍 Verifying MongoDB Replica Set Setup..."
echo ""

# Check if MongoDB container is running
if ! docker ps | grep -q "muscledia-mongodb.*Up"; then
    echo "❌ MongoDB container is not running"
    echo "   Run: docker-compose up -d mongodb"
    exit 1
fi

echo "✅ MongoDB container is running"

# Test MongoDB connection
echo "🔗 Testing MongoDB connection..."
if docker exec muscledia-mongodb mongosh -u admin -p secure_mongo_password_123 --authenticationDatabase admin --eval "print('Connection successful')" > /dev/null 2>&1; then
    echo "✅ MongoDB authentication working"
else
    echo "❌ MongoDB authentication failed"
    exit 1
fi

# Check replica set status
echo "📋 Checking replica set status..."
REPLICA_STATUS=$(docker exec muscledia-mongodb mongosh -u admin -p secure_mongo_password_123 --authenticationDatabase admin --quiet --eval "
try {
    var status = rs.status();
    if (status.myState === 1) {
        print('PRIMARY');
    } else {
        print('NOT_PRIMARY');
    }
} catch(e) {
    print('NOT_INITIALIZED');
}
")

case $REPLICA_STATUS in
    "PRIMARY")
        echo "✅ Replica set is initialized and this node is PRIMARY"
        ;;
    "NOT_PRIMARY")
        echo "⚠️  Replica set is initialized but this node is not PRIMARY"
        ;;
    "NOT_INITIALIZED")
        echo "❌ Replica set is not initialized"
        echo "   Run the initialization script: docker-compose up mongodb-setup"
        exit 1
        ;;
    *)
        echo "❓ Unknown replica set status: $REPLICA_STATUS"
        ;;
esac

# Test database operations
echo "📝 Testing database operations..."
if docker exec muscledia-mongodb mongosh -u admin -p secure_mongo_password_123 --authenticationDatabase admin --quiet --eval "
use muscledia_workouts;
db.test.insertOne({test: 'verification', timestamp: new Date()});
var count = db.test.countDocuments({test: 'verification'});
db.test.deleteMany({test: 'verification'});
print('Test operations: ' + (count > 0 ? 'SUCCESS' : 'FAILED'));
" | grep -q "SUCCESS"; then
    echo "✅ Database operations working"
else
    echo "❌ Database operations failed"
    exit 1
fi

# Check connection strings
echo ""
echo "🔗 Connection String Verification:"
echo "   MongoDB URI: mongodb://admin:secure_mongo_password_123@mongodb:27017/muscledia_workouts?authSource=admin&replicaSet=rs0"

# Test from application perspective
echo ""
echo "🚀 Testing application database connections..."

# Check if services can connect to MongoDB
if docker-compose logs workout-service 2>/dev/null | grep -q "Started.*WorkoutService"; then
    echo "✅ Workout Service connected to MongoDB"
elif docker-compose logs workout-service 2>/dev/null | grep -qi "error.*mongo"; then
    echo "❌ Workout Service has MongoDB connection issues"
    echo "   Check logs: docker-compose logs workout-service"
else
    echo "⏳ Workout Service may still be starting up"
fi

if docker-compose logs gamification-service 2>/dev/null | grep -q "Started.*GamificationService"; then
    echo "✅ Gamification Service connected to MongoDB"
elif docker-compose logs gamification-service 2>/dev/null | grep -qi "error.*mongo"; then
    echo "❌ Gamification Service has MongoDB connection issues"
    echo "   Check logs: docker-compose logs gamification-service"
else
    echo "⏳ Gamification Service may still be starting up"
fi

echo ""
echo "🎉 MongoDB verification complete!"
echo ""
echo "📊 Additional commands for troubleshooting:"
echo "   View replica set status: docker exec -it muscledia-mongodb mongosh -u admin -p secure_mongo_password_123 --authenticationDatabase admin --eval 'rs.status()'"
echo "   View MongoDB logs: docker-compose logs mongodb"
echo "   View service logs: docker-compose logs workout-service gamification-service"
echo "   Restart MongoDB: docker-compose restart mongodb"
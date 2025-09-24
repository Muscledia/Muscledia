#!/bin/bash
# scripts/migrate-mongodb-with-events.sh

echo "🔄 Migrating MongoDB data with event collections..."

# Start MongoDB
docker-compose up -d mongodb
sleep 30

# Import existing data
echo "📊 Importing workout service data..."
docker exec muscledia-mongodb mongorestore --host localhost --port 27017 \
    -u admin -p adminpassword123 --authenticationDatabase admin \
    --db muscledia_workouts /tmp/backup/muscledia_workouts

echo "📊 Importing gamification service data..."
docker exec muscledia-mongodb mongorestore --host localhost --port 27017 \
    -u admin -p adminpassword123 --authenticationDatabase admin \
    --db gamification_db /tmp/backup/gamification_db

# Create event collections if they don't exist
echo "🔧 Creating event collections..."
docker exec muscledia-mongodb mongo -u admin -p adminpassword123 --authenticationDatabase admin --eval "
use muscledia_workouts
db.createCollection('workout_event_outbox')
db.workout_event_outbox.createIndex({ 'processed': 1, 'createdAt': 1 })

use gamification_db
db.createCollection('event_outbox')
db.createCollection('processed_events')
db.event_outbox.createIndex({ 'processed': 1, 'createdAt': 1 })
db.processed_events.createIndex({ 'eventId': 1 })
"

echo "✅ MongoDB migration with events completed!"
#!/bin/bash
echo "🔧 Initializing MongoDB Replica Set..."

# Wait for MongoDB to be ready
echo "⏳ Waiting for MongoDB to start..."
until mongosh --host mongodb:27017 --eval "print('MongoDB is ready')" > /dev/null 2>&1; do
    sleep 2
done

echo "📋 Configuring replica set..."

# Initialize replica set WITH AUTHENTICATION
mongosh --host mongodb:27017 -u admin -p secure_mongo_password_123 --authenticationDatabase admin --eval "
try {
    rs.status();
    print('Replica set already initialized');
} catch(e) {
    print('Initializing replica set...');
    rs.initiate({
        _id: 'rs0',
        members: [
            { _id: 0, host: 'mongodb:27017' }
        ]
    });
    
    // Wait for replica set to be ready
    while(rs.status().myState != 1) {
        print('Waiting for replica set primary...');
        sleep(1000);
    }
    
    print('Replica set initialized successfully');
}
"

echo "✅ MongoDB replica set setup complete!"
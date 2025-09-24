#!/bin/bash

BACKUP_DIR="./backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "💾 Creating backup in $BACKUP_DIR..."

# Backup MySQL
echo "📊 Backing up MySQL database..."
docker exec muscledia-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD muscledia_users > "$BACKUP_DIR/mysql_backup.sql"

# Backup MongoDB
echo "📊 Backing up MongoDB database..."
docker exec muscledia-mongodb mongodump --host localhost --port 27017 -u admin -p $MONGO_ROOT_PASSWORD --authenticationDatabase admin --db muscledia_workouts --out /tmp/backup
docker cp muscledia-mongodb:/tmp/backup "$BACKUP_DIR/mongodb_backup"

# Backup Redis (if needed)
echo "📊 Backing up Redis data..."
docker exec muscledia-redis redis-cli --rdb /tmp/dump.rdb
docker cp muscledia-redis:/tmp/dump.rdb "$BACKUP_DIR/redis_backup.rdb"

echo "✅ Backup completed in $BACKUP_DIR"
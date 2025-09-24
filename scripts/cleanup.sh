#!/bin/bash

echo "🧹 Cleaning up Muscledia environment..."

# Stop and remove containers
docker-compose down -v

# Remove orphaned containers
docker-compose down --remove-orphans

# Remove unused networks
docker network prune -f

# Remove unused volumes (optional - uncomment if you want to reset data)
# docker volume prune -f

# Remove unused images (optional)
# docker image prune -f

echo "✅ Cleanup completed!"
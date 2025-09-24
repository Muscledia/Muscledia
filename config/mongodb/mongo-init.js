// Switch to admin database for authentication
db = db.getSiblingDB('admin');

// Create databases
db = db.getSiblingDB('muscledia_workouts');
db = db.getSiblingDB('gamification_db');

print('MongoDB databases initialized');
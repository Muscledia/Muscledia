-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS muscledia;


-- Grant permissions
GRANT ALL PRIVILEGES ON muscledia.* TO 'springstudent'@'%';

FLUSH PRIVILEGES;
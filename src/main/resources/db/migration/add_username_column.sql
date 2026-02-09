-- Migration script to add username column to users table
-- Run this script if you have existing data and want to preserve it

-- Step 1: Add username column (nullable first to allow existing data)
ALTER TABLE users ADD COLUMN username VARCHAR(20) UNIQUE AFTER id;

-- Step 2: Update existing users with a default username based on email
-- This is a temporary solution - you should update these manually with proper usernames
UPDATE users SET username = SUBSTRING_INDEX(email, '@', 1) WHERE username IS NULL;

-- Step 3: Make username NOT NULL after all records have values
ALTER TABLE users MODIFY COLUMN username VARCHAR(20) NOT NULL;

-- Note: If you're starting fresh, you can simply drop and recreate the database
-- and let Hibernate create the schema with the username column included.

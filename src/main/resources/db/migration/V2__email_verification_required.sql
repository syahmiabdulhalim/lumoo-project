-- Login requires a verified email only for accounts registered from now on (existing users keep access).
ALTER TABLE users ADD COLUMN email_verification_required bit NOT NULL DEFAULT 0;

-- Reset/verification tokens are now stored as SHA-256 hashes; any raw tokens issued before this
-- release can no longer match, so clear them (affected users simply request a new link).
UPDATE users SET reset_token = NULL, token_expiry = NULL, email_verification_token = NULL;

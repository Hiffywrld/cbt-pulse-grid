UPDATE users
SET first_name = 'System',
    last_name = 'Administrator'
WHERE lower(email) = 'admin@cbtpulse.local';

ALTER TABLE users
    ALTER COLUMN first_name TYPE VARCHAR(100),
    ALTER COLUMN last_name TYPE VARCHAR(100),
    ALTER COLUMN registration_number TYPE VARCHAR(100),
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;

CREATE UNIQUE INDEX uq_users_email_ci
    ON users (lower(email));

CREATE UNIQUE INDEX uq_users_institution_registration_ci
    ON users (institution_id, upper(registration_number))
    WHERE institution_id IS NOT NULL
      AND registration_number IS NOT NULL;

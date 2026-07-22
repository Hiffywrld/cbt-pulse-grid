CREATE TABLE institutions (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_institutions_code UNIQUE (code),
    CONSTRAINT chk_institutions_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    institution_id UUID,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    registration_number VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_users_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_institution_registration
        UNIQUE (institution_id, registration_number),
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_roles_role
        CHECK (role IN (
            'SUPER_ADMIN',
            'INSTITUTION_ADMIN',
            'EXAMINER',
            'INVIGILATOR',
            'STUDENT'
        ))
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_subjects_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT uq_subjects_institution_code UNIQUE (institution_id, code),
    CONSTRAINT chk_subjects_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    created_by UUID NOT NULL,
    question_text TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    marks NUMERIC(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_questions_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_questions_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_questions_creator
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_questions_type
        CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT chk_questions_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_questions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_questions_marks CHECK (marks > 0)
);

CREATE TABLE question_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    option_text TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT uq_question_options_display_order UNIQUE (question_id, display_order)
);

CREATE INDEX idx_subjects_institution_status
    ON subjects (institution_id, status);

CREATE INDEX idx_questions_institution
    ON questions (institution_id);

CREATE INDEX idx_questions_subject
    ON questions (subject_id);

CREATE INDEX idx_questions_institution_status
    ON questions (institution_id, status);

CREATE INDEX idx_questions_institution_difficulty
    ON questions (institution_id, difficulty);

CREATE INDEX idx_questions_institution_type
    ON questions (institution_id, type);

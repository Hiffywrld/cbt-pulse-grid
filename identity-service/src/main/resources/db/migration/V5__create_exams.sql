CREATE TABLE exams (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    created_by UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT,
    duration_minutes INTEGER NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    access_pin_hash VARCHAR(100) NOT NULL,
    shuffle_questions BOOLEAN NOT NULL,
    shuffle_options BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exams_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_exams_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_exams_creator
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_exams_institution_code UNIQUE (institution_id, code),
    CONSTRAINT chk_exams_duration CHECK (duration_minutes BETWEEN 1 AND 480),
    CONSTRAINT chk_exams_window CHECK (
        starts_at < ends_at
        AND ends_at - starts_at >= duration_minutes * INTERVAL '1 minute'
    ),
    CONSTRAINT chk_exams_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'CLOSED'))
);

CREATE TABLE exam_pool_rules (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    question_count INTEGER NOT NULL,
    marks_per_question NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_exam_pool_rules_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE,
    CONSTRAINT uq_exam_pool_rules_exam_difficulty UNIQUE (exam_id, difficulty),
    CONSTRAINT chk_exam_pool_rules_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_exam_pool_rules_question_count CHECK (question_count > 0),
    CONSTRAINT chk_exam_pool_rules_marks CHECK (marks_per_question > 0)
);

CREATE TABLE exam_candidates (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL,
    user_id UUID NOT NULL,
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exam_candidates_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_candidates_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_exam_candidates_assigner
        FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_exam_candidates_exam_user UNIQUE (exam_id, user_id)
);

CREATE INDEX idx_exams_institution ON exams (institution_id);
CREATE INDEX idx_exams_subject ON exams (subject_id);
CREATE INDEX idx_exams_institution_status ON exams (institution_id, status);
CREATE INDEX idx_exams_institution_starts_at ON exams (institution_id, starts_at);
CREATE INDEX idx_exam_candidates_exam_assigned_at ON exam_candidates (exam_id, assigned_at DESC);
CREATE INDEX idx_exam_candidates_user ON exam_candidates (user_id);

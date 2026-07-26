ALTER TABLE exams
    ADD COLUMN pass_mark_percentage NUMERIC(5, 2) NOT NULL DEFAULT 50.00,
    ADD CONSTRAINT chk_exams_pass_mark_percentage
        CHECK (pass_mark_percentage BETWEEN 0 AND 100);

CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    device_id_hash VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    last_saved_at TIMESTAMPTZ,
    score NUMERIC(12, 2),
    maximum_score NUMERIC(12, 2),
    percentage NUMERIC(5, 2),
    passed BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exam_attempts_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_exam_attempts_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_exam_attempts_candidate
        FOREIGN KEY (candidate_id) REFERENCES users (id),
    CONSTRAINT uq_exam_attempts_exam_candidate UNIQUE (exam_id, candidate_id),
    CONSTRAINT chk_exam_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED')),
    CONSTRAINT chk_exam_attempts_scores CHECK (
        (score IS NULL AND maximum_score IS NULL AND percentage IS NULL AND passed IS NULL)
        OR (
            score IS NOT NULL AND score >= 0
            AND maximum_score IS NOT NULL AND maximum_score >= 0
            AND percentage IS NOT NULL AND percentage BETWEEN 0 AND 100
            AND passed IS NOT NULL
        )
    )
);

CREATE TABLE attempt_questions (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    source_question_id UUID NOT NULL,
    position INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    marks NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_attempt_questions_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_questions_source
        FOREIGN KEY (source_question_id) REFERENCES questions (id),
    CONSTRAINT uq_attempt_questions_position UNIQUE (attempt_id, position),
    CONSTRAINT uq_attempt_questions_source UNIQUE (attempt_id, source_question_id),
    CONSTRAINT chk_attempt_questions_position CHECK (position > 0),
    CONSTRAINT chk_attempt_questions_type
        CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT chk_attempt_questions_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_attempt_questions_marks CHECK (marks > 0)
);

CREATE TABLE attempt_options (
    id UUID PRIMARY KEY,
    attempt_question_id UUID NOT NULL,
    source_option_id UUID,
    option_text TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    correct BOOLEAN NOT NULL,
    CONSTRAINT fk_attempt_options_question
        FOREIGN KEY (attempt_question_id) REFERENCES attempt_questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_options_source
        FOREIGN KEY (source_option_id) REFERENCES question_options (id) ON DELETE SET NULL,
    CONSTRAINT uq_attempt_options_display_order
        UNIQUE (attempt_question_id, display_order),
    CONSTRAINT chk_attempt_options_display_order CHECK (display_order > 0)
);

CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    attempt_question_id UUID NOT NULL,
    client_sequence BIGINT NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attempt_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question
        FOREIGN KEY (attempt_question_id) REFERENCES attempt_questions (id) ON DELETE CASCADE,
    CONSTRAINT uq_attempt_answers_attempt_question UNIQUE (attempt_id, attempt_question_id),
    CONSTRAINT chk_attempt_answers_sequence CHECK (client_sequence >= 0)
);

CREATE TABLE attempt_answer_selections (
    attempt_answer_id UUID NOT NULL,
    attempt_option_id UUID NOT NULL,
    PRIMARY KEY (attempt_answer_id, attempt_option_id),
    CONSTRAINT fk_attempt_answer_selections_answer
        FOREIGN KEY (attempt_answer_id) REFERENCES attempt_answers (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answer_selections_option
        FOREIGN KEY (attempt_option_id) REFERENCES attempt_options (id) ON DELETE CASCADE
);

CREATE TABLE attempt_sync_batches (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    sync_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attempt_sync_batches_attempt
        FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT uq_attempt_sync_batches_attempt_sync UNIQUE (attempt_id, sync_id)
);

CREATE INDEX idx_exam_attempts_candidate ON exam_attempts (candidate_id, status);
CREATE INDEX idx_exam_attempts_exam ON exam_attempts (exam_id);
CREATE INDEX idx_exam_attempts_expiry ON exam_attempts (status, expires_at);
CREATE INDEX idx_attempt_questions_attempt ON attempt_questions (attempt_id, position);
CREATE INDEX idx_attempt_options_question ON attempt_options (attempt_question_id, display_order);
CREATE INDEX idx_attempt_answers_attempt ON attempt_answers (attempt_id);
CREATE INDEX idx_attempt_answer_selections_option ON attempt_answer_selections (attempt_option_id);
CREATE INDEX idx_attempt_sync_batches_attempt ON attempt_sync_batches (attempt_id, received_at);

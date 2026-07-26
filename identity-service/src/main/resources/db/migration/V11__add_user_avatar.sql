ALTER TABLE users
    ADD COLUMN avatar_key varchar(32);

ALTER TABLE users
    ADD CONSTRAINT ck_users_avatar_key CHECK (
        avatar_key IS NULL OR avatar_key IN (
            'emerald-orbit', 'amber-sun', 'indigo-wave', 'coral-arc',
            'teal-grid', 'violet-bloom', 'navy-pulse', 'rose-kite',
            'forest-ring', 'golden-node', 'cyan-path', 'plum-spark'
        )
    );

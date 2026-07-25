CREATE TABLE IF NOT EXISTS ipc_log (
    id          BIGSERIAL    PRIMARY KEY,
    ipc_id      VARCHAR(50)  NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    message_id  VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP    NOT NULL,
    received_at TIMESTAMP    NOT NULL DEFAULT now(),
    data        JSONB        NOT NULL,
    status      VARCHAR(20)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_ipc_log_message_id'
    ) THEN
        ALTER TABLE ipc_log
            ADD CONSTRAINT uq_ipc_log_message_id UNIQUE (message_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ipc_log_occurred_at
    ON ipc_log (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_ipc_log_ipc_id_occurred_at
    ON ipc_log (ipc_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_ipc_log_type_occurred_at
    ON ipc_log (type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_ipc_log_data_gin
    ON ipc_log USING GIN (data);

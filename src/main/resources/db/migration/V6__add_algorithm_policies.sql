CREATE TABLE algorithm_policies (
    algorithm_id VARCHAR(50) NOT NULL PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
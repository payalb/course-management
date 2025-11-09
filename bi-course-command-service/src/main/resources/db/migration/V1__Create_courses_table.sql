-- Create courses table
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DOUBLE PRECISION NOT NULL,
    tags TEXT[],
    instructor_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on status for filtering
CREATE INDEX idx_courses_status ON courses(status);

-- Create index on instructor_id for queries
CREATE INDEX idx_courses_instructor ON courses(instructor_id);

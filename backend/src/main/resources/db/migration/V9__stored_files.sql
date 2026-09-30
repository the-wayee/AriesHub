-- 文件归属及元数据；对象本体由私有 S3/OSS bucket 存储。
CREATE TABLE stored_files (
    id varchar(36) PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES users(id),
    purpose varchar(20) NOT NULL CHECK (purpose IN ('AVATAR', 'ATTACHMENT')),
    object_key varchar(500) NOT NULL UNIQUE,
    filename varchar(180) NOT NULL,
    content_type varchar(100) NOT NULL,
    size bigint NOT NULL CHECK (size > 0 AND size <= 20971520),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);
CREATE INDEX stored_files_owner_idx ON stored_files(owner_id, created_at DESC) WHERE is_deleted = false;

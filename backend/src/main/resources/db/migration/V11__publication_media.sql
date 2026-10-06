ALTER TABLE stored_files DROP CONSTRAINT stored_files_size_check;
ALTER TABLE stored_files ADD CONSTRAINT stored_files_size_check CHECK(size > 0 AND size <= 104857600);
ALTER TABLE publications ADD COLUMN cover_file_id varchar(36) REFERENCES stored_files(id);
ALTER TABLE publications ADD COLUMN featured boolean NOT NULL DEFAULT false;
CREATE TABLE publication_assets (
 file_id varchar(36) PRIMARY KEY REFERENCES stored_files(id), owner_id bigint NOT NULL REFERENCES users(id),
 kind varchar(16) NOT NULL CHECK(kind IN ('COVER','IMAGE','VIDEO','ATTACHMENT')),
 filename varchar(255) NOT NULL, content_type varchar(100) NOT NULL, size bigint NOT NULL CHECK(size > 0),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE publication_media_bindings (
 publication_id bigint NOT NULL REFERENCES publications(id), file_id varchar(36) NOT NULL REFERENCES publication_assets(file_id),
 publicly_visible boolean NOT NULL DEFAULT false, PRIMARY KEY(publication_id,file_id)
);
CREATE INDEX publication_assets_owner_idx ON publication_assets(owner_id,created_at DESC);

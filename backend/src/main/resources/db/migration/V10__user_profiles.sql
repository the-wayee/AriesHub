ALTER TABLE users ADD COLUMN bio varchar(160) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN avatar_file_id varchar(36) REFERENCES stored_files(id);

COMMENT ON COLUMN users.bio IS '用户个性签名，最多 160 字符';
COMMENT ON COLUMN users.avatar_file_id IS '当前头像文件 ID；不保存临时签名 URL';

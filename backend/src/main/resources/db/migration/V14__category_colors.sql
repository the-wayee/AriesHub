ALTER TABLE categories ADD COLUMN color varchar(7) NOT NULL DEFAULT '#4967A9';
ALTER TABLE categories ADD CONSTRAINT categories_color_hex CHECK (color ~ '^#[0-9A-Fa-f]{6}$');

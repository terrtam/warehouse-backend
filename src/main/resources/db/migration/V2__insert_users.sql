-- V2__insert_users.sql

INSERT INTO users (id, username, password_hash, role)
VALUES
    (gen_random_uuid(), 'manager1', '$2a$10$p.KWHqorpeBr9i9FQPZsbuJXNJ0hKsQlYmkV5D2sjnpDbBb85kW5a', 'MANAGER'),
    (gen_random_uuid(), 'staff1',   '$2a$10$EA0lXhIdi6YlLAHztG9I6OghQcG2G13XDPp/ULAkPnpJAEQR8SWn.', 'STAFF');

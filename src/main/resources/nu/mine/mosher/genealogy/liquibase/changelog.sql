--liquibase formatted sql

/*
permissions (hierarchical enum)

PUBLIC
PRIVATE (implies PUBLIC)
LIST (implies PUBLIC)
READ (implies LIST)

roles (have zero or more permissions)

UNAUTHORIZED --> [no permissions]
GUEST --> LIST
UNTRUSTED --> READ
TRUSTED --> READ, PRIVATE

default_uesrs

<default role for unauthenticated users>: GUEST
<default role for authenticated users (if users.role is NULL)>: UNTRUSTED



VALID = request has valid authenticated identifier (google email from "idtoken" JWT in header)

if VALID and user does not exist in user table
    insert user into user table
endif

if VALID
    role = read user's default role from database
        or <default role if users.role is NULL>
else
    role = <default role for unauthenticated users>
endif
*/


--changeset permissions:1
CREATE TABLE permissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

--changeset permission_implies:2
CREATE TABLE permission_implies (
    permission INTEGER NOT NULL REFERENCES permissions,
    implies INTEGER NOT NULL REFERENCES permissions
);

--changeset all_permissions:3
CREATE VIEW all_permissions AS
WITH RECURSIVE
    all_perms(oid, id, name, implies)
AS (
    SELECT p.id, p.id, p.name, i.implies
    FROM
        permissions AS p LEFT OUTER JOIN
        permission_implies AS i ON (i.permission = p.id)
    UNION ALL
    SELECT a.oid, p.id, p.name, i.implies
    FROM
        all_perms AS a JOIN
        permissions AS p ON (p.id = a.implies) LEFT OUTER JOIN
        permission_implies AS i ON (i.permission = p.id)
) SELECT oid, id FROM all_perms GROUP BY oid, id;

--changeset roles:4
CREATE TABLE roles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

--changeset role_permissions:5
CREATE TABLE role_permissions (
    role INTEGER NOT NULL REFERENCES roles,
    permission INTEGER NOT NULL REFERENCES permissions
);

--changeset role_all_permissions:6
CREATE VIEW role_all_permissions AS
SELECT r.id AS role, p.id AS permission
FROM
roles AS r JOIN
role_permissions AS rp ON (rp.role = r.id) JOIN
all_permissions AS p ON (p.oid = rp.permission);

--changeset role_all_permission_names:7
CREATE VIEW role_all_permission_names AS
SELECT role, name
FROM
role_all_permissions AS r JOIN
permissions AS p ON (p.id = r.permission)
GROUP BY role, name;

--changeset user_defaults:8
CREATE TABLE user_defaults (
    type TEXT NOT NULL PRIMARY KEY,
    role INTEGER REFERENCES roles
);

--changeset users:9
CREATE TABLE users (
    email TEXT PRIMARY KEY,
    gid TEXT UNIQUE,
    role INTEGER REFERENCES roles
);

-- If user is authenticated, then a NULL users.role indicates to use:
--     user_defaults.role WHERE name = 'AUTHENTICATED'
-- To change this (e.g., to grant a user more access):
--     UPDATE users SET role = (SELECT id FROM roles WHERE name = 'TRUSTED') WHERE email = 'xxx';
-- To ban a user:
--     UPDATE users SET role = (SELECT id FROM roles WHERE name = 'UNAUTHORIZED') WHERE email = 'xxx';

--changeset all_unauthenticated_permission_names:10
CREATE VIEW all_unauthenticated_permission_names AS
SELECT name
FROM
user_defaults AS u JOIN
role_all_permission_names AS n ON (n.role = u.role)
WHERE
u.type = 'UNAUTHENTICATED';
--select * from all_unauthenticated_permission_names

--insert into users values ('cmosher','abc',4)
--insert into users values ('other','xyz',NULL)
--select * from users

--changeset all_authenticated_permission_names:11
CREATE VIEW all_authenticated_permission_names AS
SELECT email, gid, p.name
FROM
users AS u LEFT OUTER JOIN
role_all_permission_names AS p ON (p.role = COALESCE(u.role,(SELECT role FROM user_defaults WHERE type = 'AUTHENTICATED')));
--select * from all_authenticated_permission_names

--changeset requests:12
CREATE TABLE requests (
    ts TEXT PRIMARY KEY,
    ip TEXT,
    agent TEXT,
    uri TEXT,
    person TEXT,
    tree TEXT,
    user INTEGER REFERENCES users
);




--changeset foundational_data:11

-- these are enums in the application and their meaning is hardcoded into the application
INSERT INTO permissions (name) VALUES ('PUBLIC');  -- 1
INSERT INTO permissions (name) VALUES ('PRIVATE'); -- 2
INSERT INTO permissions (name) VALUES ('LIST');    -- 3
INSERT INTO permissions (name) VALUES ('READ');    -- 4
INSERT INTO permissions (name) VALUES ('CONFIG');  -- 5

INSERT INTO permission_implies VALUES (2, 1); -- PRIVATE implies PUBLIC
INSERT INTO permission_implies VALUES (3, 1); -- LIST implies PUBLIC
INSERT INTO permission_implies VALUES (4, 3); -- READ implies LIST

INSERT INTO roles (name) VALUES ('UNAUTHORIZED'); -- 1
INSERT INTO roles (name) VALUES ('GUEST');        -- 2
INSERT INTO roles (name) VALUES ('UNTRUSTED');    -- 3
INSERT INTO roles (name) VALUES ('TRUSTED');      -- 4
INSERT INTO roles (name) VALUES ('ADMIN');        -- 5

-- (note: role UNAUTHORIZED has no permissions)
INSERT INTO role_permissions VALUES (2, 3); -- GUEST: LIST
INSERT INTO role_permissions VALUES (3, 4); -- UNTRUSTED: READ
INSERT INTO role_permissions VALUES (4, 4); -- TRUSTED: READ
INSERT INTO role_permissions VALUES (4, 2); -- TRUSTED: PRIVATE
INSERT INTO role_permissions VALUES (5, 4); -- ADMIN: READ
INSERT INTO role_permissions VALUES (5, 2); -- ADMIN: PRIVATE
INSERT INTO role_permissions VALUES (5, 5); -- ADMIN: CONFIG

INSERT INTO user_defaults (type, role) VALUES ('UNAUTHENTICATED', 2); -- GUEST
INSERT INTO user_defaults (type, role) VALUES ('AUTHENTICATED', 3);   -- UNTRUSTED

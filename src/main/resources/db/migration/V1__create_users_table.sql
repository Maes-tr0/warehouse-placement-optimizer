CREATE SEQUENCE IF NOT EXISTS seq_user
    START WITH 1
    INCREMENT BY 1
;

CREATE TABLE IF NOT EXISTS users
(
    id              BIGINT       NOT NULL DEFAULT nextval('seq_user')
    ,email          VARCHAR(255) NOT NULL UNIQUE
    ,password       VARCHAR(255) NOT NULL
    ,full_name      VARCHAR(255) NOT NULL
    ,role           VARCHAR(50)  NOT NULL
    ,status         VARCHAR(50)  NOT NULL
    ,created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    ,updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER SEQUENCE seq_user OWNED BY users.id;
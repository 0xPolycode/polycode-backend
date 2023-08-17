CREATE DOMAIN polyflow.POLYFLOW_USER_ID AS UUID;
CREATE TYPE polyflow.USER_ACCOUNT_TYPE AS ENUM ('EMAIL_REGISTERED');

CREATE TABLE IF NOT EXISTS polyflow.user (
    id                              POLYFLOW_USER_ID         NOT NULL PRIMARY KEY,
    email                           VARCHAR                  NOT NULL UNIQUE,
    password_hash                   VARCHAR                  NOT NULL,
    account_type                    USER_ACCOUNT_TYPE        NOT NULL,
    registered_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at                     TIMESTAMP WITH TIME ZONE     NULL,
    stripe_customer_id              VARCHAR                      NULL UNIQUE,
    total_domain_limit              INT                      NOT NULL,
    total_seat_limit                INT                      NOT NULL,
    stripe_session_id               VARCHAR                      NULL,
    monthly_polycode_read_requests  BIGINT                   NOT NULL,
    monthly_polycode_write_requests BIGINT                   NOT NULL
);

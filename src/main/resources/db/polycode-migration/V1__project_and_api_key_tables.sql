CREATE TYPE polycode.USER_IDENTIFIER_TYPE AS ENUM ('ETH_WALLET_ADDRESS', 'POLYFLOW_USER_ID');

CREATE DOMAIN polycode.USER_ID AS UUID;

CREATE TABLE polycode.user_identifier (
    id              USER_ID              NOT NULL PRIMARY KEY,
    user_identifier VARCHAR              NOT NULL,
    identifier_type USER_IDENTIFIER_TYPE NOT NULL,
    UNIQUE (user_identifier, identifier_type)
);

CREATE DOMAIN polycode.PROJECT_ID AS UUID;

CREATE TABLE polycode.project (
    id                PROJECT_ID               NOT NULL PRIMARY KEY,
    owner_id          USER_ID                  NOT NULL REFERENCES polycode.user_identifier(id),
    base_redirect_url VARCHAR                  NOT NULL,
    chain_id          BIGINT                   NOT NULL,
    custom_rpc_url    VARCHAR                      NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.project(owner_id);
CREATE INDEX ON polycode.project(created_at);

CREATE DOMAIN polycode.API_KEY_ID AS UUID;

CREATE TABLE polycode.api_key (
    id         API_KEY_ID               NOT NULL PRIMARY KEY,
    project_id PROJECT_ID               NOT NULL REFERENCES polycode.project(id),
    api_key    VARCHAR                  NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.api_key(project_id);
CREATE INDEX ON polycode.api_key(api_key);
CREATE INDEX ON polycode.api_key(created_at);

CREATE DOMAIN polycode.API_USAGE_PERIOD_ID AS UUID;

CREATE TABLE polycode.api_usage_period (
    id                     API_USAGE_PERIOD_ID      NOT NULL PRIMARY KEY,
    user_id                USER_ID                  NOT NULL REFERENCES polycode.user_identifier(id),
    allowed_write_requests BIGINT                   NOT NULL,
    allowed_read_requests  BIGINT                   NOT NULL,
    used_write_requests    BIGINT                   NOT NULL,
    used_read_requests     BIGINT                   NOT NULL,
    start_date             TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date               TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (end_date > start_date)
);

CREATE INDEX ON polycode.api_usage_period(user_id, end_date);
CREATE INDEX ON polycode.api_usage_period(user_id, start_date, end_date);

CREATE TYPE polycode.REQUEST_METHOD AS ENUM ('GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'TRACE');

CREATE TABLE polycode.api_write_call (
    user_id        USER_ID                  NOT NULL REFERENCES polycode.user_identifier(id),
    request_method REQUEST_METHOD           NOT NULL,
    request_path   VARCHAR                  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.api_write_call(user_id, created_at);

CREATE TABLE polycode.api_read_call (
    user_id      USER_ID                  NOT NULL REFERENCES polycode.user_identifier(id),
    request_path VARCHAR                  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.api_read_call(user_id, created_at);

CREATE DOMAIN polycode.ERC20_LOCK_REQUEST_ID AS UUID;

CREATE TABLE polycode.erc20_lock_request (
    id                           ERC20_LOCK_REQUEST_ID    NOT NULL PRIMARY KEY,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    token_address                VARCHAR                  NOT NULL,
    token_amount                 NUMERIC(78)              NOT NULL,
    lock_duration_seconds        NUMERIC(78)              NOT NULL,
    lock_contract_address        VARCHAR                  NOT NULL,
    token_sender_address         VARCHAR                      NULL,
    arbitrary_data               JSON                         NULL,
    tx_hash                      VARCHAR                      NULL,
    screen_before_action_message VARCHAR                      NULL,
    screen_after_action_message  VARCHAR                      NULL,
    project_id                   PROJECT_ID               NOT NULL REFERENCES polycode.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.erc20_lock_request(project_id);
CREATE INDEX ON polycode.erc20_lock_request(created_at);

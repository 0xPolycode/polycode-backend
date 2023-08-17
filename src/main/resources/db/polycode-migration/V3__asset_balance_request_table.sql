CREATE DOMAIN polycode.ASSET_BALANCE_REQUEST_ID AS UUID;

CREATE TABLE polycode.asset_balance_request (
    id                           ASSET_BALANCE_REQUEST_ID NOT NULL PRIMARY KEY,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    token_address                VARCHAR                      NULL,
    block_number                 NUMERIC(78)                  NULL,
    requested_wallet_address     VARCHAR                      NULL,
    arbitrary_data               JSON                         NULL,
    actual_wallet_address        VARCHAR                      NULL,
    signed_message               VARCHAR                      NULL,
    screen_before_action_message VARCHAR                      NULL,
    screen_after_action_message  VARCHAR                      NULL,
    project_id                   PROJECT_ID               NOT NULL REFERENCES polycode.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.asset_balance_request(project_id);
CREATE INDEX ON polycode.asset_balance_request(created_at);

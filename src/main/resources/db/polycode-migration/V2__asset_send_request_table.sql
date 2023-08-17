CREATE DOMAIN polycode.ASSET_SEND_REQUEST_ID AS UUID;

CREATE TABLE polycode.asset_send_request (
    id                           ASSET_SEND_REQUEST_ID    NOT NULL PRIMARY KEY,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    token_address                VARCHAR                      NULL,
    asset_amount                 NUMERIC(78)              NOT NULL,
    asset_sender_address         VARCHAR                      NULL,
    asset_recipient_address      VARCHAR                  NOT NULL,
    arbitrary_data               JSON                         NULL,
    tx_hash                      VARCHAR                      NULL,
    screen_before_action_message VARCHAR                      NULL,
    screen_after_action_message  VARCHAR                      NULL,
    project_id                   PROJECT_ID               NOT NULL REFERENCES polycode.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.asset_send_request(asset_sender_address);
CREATE INDEX ON polycode.asset_send_request(asset_recipient_address);
CREATE INDEX ON polycode.asset_send_request(project_id);
CREATE INDEX ON polycode.asset_send_request(created_at);

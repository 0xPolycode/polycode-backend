CREATE DOMAIN polycode.ASSET_MULTI_SEND_REQUEST_ID AS UUID;

CREATE TABLE polycode.asset_multi_send_request (
    id                                    ASSET_MULTI_SEND_REQUEST_ID NOT NULL PRIMARY KEY,
    chain_id                              BIGINT                      NOT NULL,
    redirect_url                          VARCHAR                     NOT NULL,
    token_address                         VARCHAR                         NULL,
    disperse_contract_address             VARCHAR                     NOT NULL,
    asset_amounts                         NUMERIC(78)[]               NOT NULL,
    asset_recipient_addresses             VARCHAR[]                   NOT NULL,
    item_names                            VARCHAR[]                   NOT NULL,
    asset_sender_address                  VARCHAR                         NULL,
    arbitrary_data                        JSON                            NULL,
    approve_tx_hash                       VARCHAR                         NULL,
    disperse_tx_hash                      VARCHAR                         NULL,
    approve_screen_before_action_message  VARCHAR                         NULL,
    approve_screen_after_action_message   VARCHAR                         NULL,
    disperse_screen_before_action_message VARCHAR                         NULL,
    disperse_screen_after_action_message  VARCHAR                         NULL,
    project_id                            PROJECT_ID                  NOT NULL REFERENCES polycode.project(id),
    created_at                            TIMESTAMP WITH TIME ZONE    NOT NULL,
    CHECK (
        array_length(asset_amounts, 1) = array_length(asset_recipient_addresses, 1) AND
        array_length(asset_amounts, 1) = array_length(item_names, 1)
    )
);

CREATE INDEX ON polycode.asset_multi_send_request(project_id);
CREATE INDEX ON polycode.asset_multi_send_request(created_at);
CREATE INDEX ON polycode.asset_multi_send_request(asset_sender_address);

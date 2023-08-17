CREATE DOMAIN polycode.AUTHORIZATION_REQUEST_ID AS UUID;

CREATE TABLE polycode.authorization_request (
    id                           AUTHORIZATION_REQUEST_ID NOT NULL PRIMARY KEY,
    project_id                   PROJECT_ID               NOT NULL REFERENCES polycode.project(id),
    redirect_url                 VARCHAR                  NOT NULL,
    requested_wallet_address     VARCHAR                      NULL,
    actual_wallet_address        VARCHAR                      NULL,
    signed_message               VARCHAR                      NULL,
    arbitrary_data               JSON                         NULL,
    screen_before_action_message VARCHAR                      NULL,
    screen_after_action_message  VARCHAR                      NULL,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    message_to_sign_override     VARCHAR                      NULL,
    store_indefinitely           BOOLEAN                  NOT NULL
);

CREATE INDEX ON polycode.authorization_request(project_id);
CREATE INDEX ON polycode.authorization_request(created_at);

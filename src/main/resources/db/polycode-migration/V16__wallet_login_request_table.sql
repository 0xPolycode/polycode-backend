CREATE DOMAIN polycode.WALLET_LOGIN_REQUEST_ID AS UUID;

CREATE TABLE polycode.wallet_login_request (
    id              WALLET_LOGIN_REQUEST_ID  NOT NULL PRIMARY KEY,
    wallet_address  VARCHAR                  NOT NULL,
    message_to_sign VARCHAR                  NOT NULL,
    signed_message  VARCHAR                      NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ON polycode.wallet_login_request(created_at);

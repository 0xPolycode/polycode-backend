CREATE DOMAIN polycode.ADDRESS_BOOK_ID AS UUID;

CREATE TABLE polycode.address_book (
    id             ADDRESS_BOOK_ID          NOT NULL PRIMARY KEY,
    alias          VARCHAR                  NOT NULL,
    wallet_address VARCHAR                  NOT NULL,
    phone_number   VARCHAR                      NULL,
    email          VARCHAR                      NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id        USER_ID                  NOT NULL REFERENCES polycode.user_identifier(id),
    UNIQUE (user_id, alias)
);

CREATE INDEX ON polycode.address_book(user_id);

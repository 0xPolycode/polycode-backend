CREATE DOMAIN polycode.MULTI_PAYMENT_TEMPLATE_ID AS UUID;

CREATE TABLE polycode.multi_payment_template (
    id            MULTI_PAYMENT_TEMPLATE_ID NOT NULL PRIMARY KEY,
    template_name VARCHAR                   NOT NULL,
    chain_id      BIGINT                    NOT NULL,
    user_id       USER_ID                   NOT NULL REFERENCES polycode.user_identifier(id),
    created_at    TIMESTAMP WITH TIME ZONE  NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE      NULL,
    token_address VARCHAR                       NULL
);

CREATE INDEX ON polycode.multi_payment_template(created_at);
CREATE INDEX ON polycode.multi_payment_template(user_id);

CREATE DOMAIN polycode.MULTI_PAYMENT_TEMPLATE_ITEM_ID AS UUID;

CREATE TABLE polycode.multi_payment_template_item (
    id             MULTI_PAYMENT_TEMPLATE_ITEM_ID NOT NULL PRIMARY KEY,
    template_id    MULTI_PAYMENT_TEMPLATE_ID      NOT NULL REFERENCES polycode.multi_payment_template(id),
    wallet_address VARCHAR                        NOT NULL,
    item_name      VARCHAR                            NULL,
    asset_amount   NUMERIC(78)                    NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE       NOT NULL
);

CREATE INDEX ON polycode.multi_payment_template_item(template_id);
CREATE INDEX ON polycode.multi_payment_template_item(created_at);

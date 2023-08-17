CREATE DOMAIN polycode.FETCH_ACCOUNT_BALANCE_CACHE_ID AS UUID;

CREATE TABLE polycode.fetch_account_balance_cache (
    id             FETCH_ACCOUNT_BALANCE_CACHE_ID NOT NULL PRIMARY KEY,
    chain_id       BIGINT                         NOT NULL,
    custom_rpc_url VARCHAR                        NOT NULL,
    wallet_address VARCHAR                        NOT NULL,
    block_number   NUMERIC(78)                    NOT NULL,
    timestamp      TIMESTAMP WITH TIME ZONE       NOT NULL,
    asset_amount   NUMERIC(78)                    NOT NULL
);

CREATE UNIQUE INDEX ON polycode.fetch_account_balance_cache(chain_id, custom_rpc_url, wallet_address, block_number);

CREATE DOMAIN polycode.FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID AS UUID;

CREATE TABLE polycode.fetch_erc20_account_balance_cache (
    id               FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID NOT NULL PRIMARY KEY,
    chain_id         BIGINT                               NOT NULL,
    custom_rpc_url   VARCHAR                              NOT NULL,
    contract_address VARCHAR                              NOT NULL,
    wallet_address   VARCHAR                              NOT NULL,
    block_number     NUMERIC(78)                          NOT NULL,
    timestamp        TIMESTAMP WITH TIME ZONE             NOT NULL,
    asset_amount     NUMERIC(78)                          NOT NULL
);

CREATE UNIQUE INDEX ON polycode.fetch_erc20_account_balance_cache(
    chain_id, custom_rpc_url, contract_address, wallet_address, block_number
);

CREATE TYPE polycode.EVENT_LOG AS (
    log_data   VARCHAR,
    log_topics VARCHAR[]
);

CREATE DOMAIN polycode.FETCH_TRANSACTION_INFO_CACHE_ID AS UUID;

CREATE TABLE polycode.fetch_transaction_info_cache (
    id                        FETCH_TRANSACTION_INFO_CACHE_ID NOT NULL PRIMARY KEY,
    chain_id                  BIGINT                          NOT NULL,
    custom_rpc_url            VARCHAR                         NOT NULL,
    tx_hash                   VARCHAR                         NOT NULL,
    from_address              VARCHAR                         NOT NULL,
    to_address                VARCHAR                         NOT NULL,
    deployed_contract_address VARCHAR                             NULL,
    tx_data                   BYTEA                           NOT NULL,
    value_amount              NUMERIC(78)                     NOT NULL,
    block_number              NUMERIC(78)                     NOT NULL,
    timestamp                 TIMESTAMP WITH TIME ZONE        NOT NULL,
    success                   BOOLEAN                         NOT NULL,
    event_logs                EVENT_LOG[]                     NOT NULL
);

CREATE UNIQUE INDEX ON polycode.fetch_transaction_info_cache(chain_id, custom_rpc_url, tx_hash);

CREATE DOMAIN polycode.CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID AS UUID;

CREATE TABLE polycode.contract_deployment_transaction_cache (
    id               CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID NOT NULL PRIMARY KEY,
    chain_id         BIGINT                                   NOT NULL,
    custom_rpc_url   VARCHAR                                  NOT NULL,
    contract_address VARCHAR                                  NOT NULL,
    tx_hash          VARCHAR                                      NULL,
    from_address     VARCHAR                                      NULL,
    tx_data          BYTEA                                        NULL,
    value_amount     NUMERIC(78)                                  NULL,
    contract_binary  BYTEA                                    NOT NULL,
    block_number     NUMERIC(78)                                  NULL,
    event_logs       EVENT_LOG[]                              NOT NULL
);

CREATE UNIQUE INDEX ON polycode.contract_deployment_transaction_cache(chain_id, custom_rpc_url, contract_address);


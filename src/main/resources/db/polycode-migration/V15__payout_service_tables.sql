CREATE TYPE polycode.HASH_FUNCTION                AS ENUM ('IDENTITY', 'FIXED', 'KECCAK_256');
CREATE TYPE polycode.ASSET_SNAPSHOT_STATUS        AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE polycode.ASSET_SNAPSHOT_FAILURE_CAUSE AS ENUM ('LOG_RESPONSE_LIMIT', 'OTHER');

CREATE DOMAIN polycode.MERKLE_TREE_ROOT_ID AS UUID;

CREATE TABLE polycode.merkle_tree_root (
    id                     MERKLE_TREE_ROOT_ID    NOT NULL PRIMARY KEY,
    chain_id               BIGINT                 NOT NULL,
    asset_contract_address VARCHAR                NOT NULL,
    block_number           NUMERIC(78)            NOT NULL,
    merkle_hash            VARCHAR                NOT NULL,
    hash_fn                polycode.HASH_FUNCTION NOT NULL
);

CREATE UNIQUE INDEX ON polycode.merkle_tree_root(chain_id, asset_contract_address, merkle_hash);

CREATE DOMAIN polycode.MERKLE_TREE_LEAF_ID AS UUID;

CREATE TABLE polycode.merkle_tree_leaf_node (
    id             MERKLE_TREE_LEAF_ID NOT NULL PRIMARY KEY,
    merkle_root    MERKLE_TREE_ROOT_ID NOT NULL REFERENCES polycode.merkle_tree_root(id),
    wallet_address VARCHAR             NOT NULL,
    asset_amount   NUMERIC(78)         NOT NULL
);

CREATE INDEX ON polycode.merkle_tree_leaf_node(merkle_root);
CREATE INDEX ON polycode.merkle_tree_leaf_node(wallet_address);
CREATE UNIQUE INDEX ON polycode.merkle_tree_leaf_node(merkle_root, wallet_address);

CREATE DOMAIN polycode.ASSET_SNAPSHOT_ID AS UUID;

CREATE TABLE polycode.asset_snapshot (
    id                       ASSET_SNAPSHOT_ID            NOT NULL PRIMARY KEY,
    project_id               PROJECT_ID                   NOT NULL REFERENCES polycode.project(id),
    name                     VARCHAR                      NOT NULL,
    chain_id                 BIGINT                       NOT NULL,
    asset_contract_address   VARCHAR                      NOT NULL,
    block_number             NUMERIC(78)                  NOT NULL,
    ignored_holder_addresses VARCHAR[]                    NOT NULL,
    status                   ASSET_SNAPSHOT_STATUS        NOT NULL,
    result_tree              MERKLE_TREE_ROOT_ID              NULL REFERENCES polycode.merkle_tree_root(id),
    tree_ipfs_hash           VARCHAR                          NULL,
    total_asset_amount       NUMERIC(78)                      NULL,
    failure_cause            ASSET_SNAPSHOT_FAILURE_CAUSE     NULL
);

CREATE INDEX ON polycode.asset_snapshot(project_id, status);
CREATE INDEX ON polycode.asset_snapshot(status);

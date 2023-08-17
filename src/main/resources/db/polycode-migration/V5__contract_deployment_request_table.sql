CREATE DOMAIN polycode.CONTRACT_METADATA_ID AS UUID;

CREATE TABLE polycode.contract_metadata (
    id                  CONTRACT_METADATA_ID NOT NULL PRIMARY KEY,
    contract_id         VARCHAR              NOT NULL,
    contract_tags       VARCHAR[]            NOT NULL,
    contract_implements VARCHAR[]            NOT NULL,
    name                VARCHAR                  NULL,
    description         VARCHAR                  NULL,
    project_id          PROJECT_ID           NOT NULL,
    UNIQUE (contract_id, project_id)
);

CREATE INDEX ON polycode.contract_metadata USING gin(contract_tags);
CREATE INDEX ON polycode.contract_metadata USING gin(contract_implements);

CREATE DOMAIN polycode.CONTRACT_DEPLOYMENT_REQUEST_ID AS UUID;

CREATE TABLE polycode.contract_deployment_request (
    id                              CONTRACT_DEPLOYMENT_REQUEST_ID NOT NULL PRIMARY KEY,
    alias                           VARCHAR                        NOT NULL,
    contract_metadata_id            CONTRACT_METADATA_ID           NOT NULL REFERENCES polycode.contract_metadata(id),
    contract_data                   BYTEA                          NOT NULL,
    constructor_params              JSON                           NOT NULL,
    initial_eth_amount              NUMERIC(78)                    NOT NULL,
    chain_id                        BIGINT                         NOT NULL,
    redirect_url                    VARCHAR                        NOT NULL,
    project_id                      PROJECT_ID                     NOT NULL REFERENCES polycode.project(id),
    created_at                      TIMESTAMP WITH TIME ZONE       NOT NULL,
    arbitrary_data                  JSON                               NULL,
    screen_before_action_message    VARCHAR                            NULL,
    screen_after_action_message     VARCHAR                            NULL,
    contract_address                VARCHAR                            NULL,
    deployer_address                VARCHAR                            NULL,
    tx_hash                         VARCHAR                            NULL,
    imported                        BOOLEAN                        NOT NULL,
    deleted                         BOOLEAN                        NOT NULL,
    proxy                           BOOLEAN                        NOT NULL,
    implementation_contract_address VARCHAR                            NULL,
    UNIQUE (project_id, alias)
);

CREATE INDEX ON polycode.contract_deployment_request(project_id);
CREATE INDEX ON polycode.contract_deployment_request(created_at);
CREATE INDEX ON polycode.contract_deployment_request(contract_metadata_id);
CREATE INDEX ON polycode.contract_deployment_request(contract_address);
CREATE INDEX ON polycode.contract_deployment_request(deployer_address);
CREATE INDEX ON polycode.contract_deployment_request(deleted);

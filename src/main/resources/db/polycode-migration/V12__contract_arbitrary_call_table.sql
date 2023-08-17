CREATE DOMAIN polycode.CONTRACT_ARBITRARY_CALL_REQUEST_ID AS UUID;

CREATE TABLE polycode.contract_arbitrary_call_request (
    id                           CONTRACT_ARBITRARY_CALL_REQUEST_ID NOT NULL PRIMARY KEY,
    deployed_contract_id         CONTRACT_DEPLOYMENT_REQUEST_ID     NULL
                                 REFERENCES polycode.contract_deployment_request(id),
    contract_address             VARCHAR                            NOT NULL,
    function_data                BYTEA                              NOT NULL,
    function_name                VARCHAR                                NULL,
    function_params              JSON                                   NULL,
    eth_amount                   NUMERIC(78)                        NOT NULL,
    chain_id                     BIGINT                             NOT NULL,
    redirect_url                 VARCHAR                            NOT NULL,
    project_id                   PROJECT_ID                         NOT NULL REFERENCES polycode.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE           NOT NULL,
    arbitrary_data               JSON                                   NULL,
    screen_before_action_message VARCHAR                                NULL,
    screen_after_action_message  VARCHAR                                NULL,
    caller_address               VARCHAR                                NULL,
    tx_hash                      VARCHAR                                NULL
);

CREATE INDEX ON polycode.contract_arbitrary_call_request(project_id);
CREATE INDEX ON polycode.contract_arbitrary_call_request(created_at);
CREATE INDEX ON polycode.contract_arbitrary_call_request(deployed_contract_id);
CREATE INDEX ON polycode.contract_arbitrary_call_request(contract_address);
CREATE INDEX ON polycode.contract_arbitrary_call_request(caller_address);

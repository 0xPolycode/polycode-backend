CREATE DOMAIN polycode.IMPORTED_CONTRACT_DECORATOR_ID AS UUID;

CREATE TABLE polycode.imported_contract_decorator (
    id                  IMPORTED_CONTRACT_DECORATOR_ID NOT NULL PRIMARY KEY,
    project_id          PROJECT_ID                     NOT NULL REFERENCES polycode.project(id),
    contract_id         VARCHAR                        NOT NULL,
    manifest_json       JSON                           NOT NULL,
    artifact_json       JSON                           NOT NULL,
    info_markdown       VARCHAR                        NOT NULL,
    contract_tags       VARCHAR[]                      NOT NULL,
    contract_implements VARCHAR[]                      NOT NULL,
    imported_at         TIMESTAMP WITH TIME ZONE       NOT NULL,
    UNIQUE (project_id, contract_id)
);

CREATE INDEX ON polycode.imported_contract_decorator(project_id);
CREATE INDEX ON polycode.imported_contract_decorator USING gin(contract_tags);
CREATE INDEX ON polycode.imported_contract_decorator USING gin(contract_implements);
CREATE INDEX ON polycode.imported_contract_decorator(imported_at);

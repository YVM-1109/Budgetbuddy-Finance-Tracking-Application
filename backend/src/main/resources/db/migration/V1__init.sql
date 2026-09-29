-- BudgetBuddy initial schema (MySQL / TiDB compatible; also runs on H2 in MySQL mode)

CREATE TABLE users (
  id                      BINARY(16)     NOT NULL PRIMARY KEY,
  name                    VARCHAR(120)   NOT NULL,
  email                   VARCHAR(255)   NOT NULL,
  password_hash           VARCHAR(255)   NULL,
  auth_provider           VARCHAR(16)    NOT NULL DEFAULT 'LOCAL',
  google_subject          VARCHAR(255)   NULL,
  monthly_budget          DECIMAL(15,2)  NOT NULL DEFAULT 0,
  monthly_savings_target  DECIMAL(15,2)  NOT NULL DEFAULT 0,
  created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT uq_users_google_subject UNIQUE (google_subject),
  CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'LINKED'))
);

CREATE INDEX idx_users_google_subject ON users (google_subject);

CREATE TABLE recurring_finances (
  id            BINARY(16)     NOT NULL PRIMARY KEY,
  user_id       BINARY(16)     NOT NULL,
  type          VARCHAR(10)    NOT NULL,
  amount        DECIMAL(15,2)  NOT NULL,
  category      VARCHAR(80)    NOT NULL,
  remarks       VARCHAR(500)   NULL,
  start_date    DATE           NOT NULL,
  end_date      DATE           NULL,
  active        BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT ck_recurring_type CHECK (type IN ('INCOME', 'EXPENSE')),
  CONSTRAINT ck_recurring_amount CHECK (amount > 0)
);

CREATE INDEX idx_recurring_user ON recurring_finances (user_id, active);

CREATE TABLE transactions (
  id                     BINARY(16)     NOT NULL PRIMARY KEY,
  user_id                BINARY(16)     NOT NULL,
  type                   VARCHAR(10)    NOT NULL,
  amount                 DECIMAL(15,2)  NOT NULL,
  transaction_date       DATE           NOT NULL,
  category               VARCHAR(80)    NOT NULL,
  remarks                VARCHAR(500)   NULL,
  source_type            VARCHAR(16)    NOT NULL DEFAULT 'MANUAL',
  recurring_finance_id   BINARY(16)     NULL,
  created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_tx_recurring FOREIGN KEY (recurring_finance_id) REFERENCES recurring_finances (id),
  CONSTRAINT ck_tx_type CHECK (type IN ('INCOME', 'EXPENSE')),
  CONSTRAINT ck_tx_source CHECK (source_type IN ('MANUAL', 'RECURRING')),
  CONSTRAINT ck_tx_amount CHECK (amount > 0)
);

CREATE INDEX idx_tx_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_tx_user_type_date ON transactions (user_id, type, transaction_date);
CREATE INDEX idx_tx_user_category_date ON transactions (user_id, category, transaction_date);
CREATE INDEX idx_tx_recurring_date ON transactions (recurring_finance_id, transaction_date);

CREATE TABLE recurring_generation_records (
  id                          BINARY(16)  NOT NULL PRIMARY KEY,
  recurring_finance_id        BINARY(16)  NOT NULL,
  period_year                 INT         NOT NULL,
  period_month                INT         NOT NULL,
  generated_transaction_id    BINARY(16)  NOT NULL,
  generated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_gen_recurring FOREIGN KEY (recurring_finance_id) REFERENCES recurring_finances (id),
  CONSTRAINT fk_gen_tx FOREIGN KEY (generated_transaction_id) REFERENCES transactions (id),
  CONSTRAINT uq_gen_recurring_period UNIQUE (recurring_finance_id, period_year, period_month)
);

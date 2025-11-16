-- Merged Flyway migration: All tables and indexes (H2 compatible)

-- Table: savings_account
CREATE TABLE IF NOT EXISTS savings_account (
  account_number BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  personal_id BIGINT NOT NULL,
  balance BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  last_updated TIMESTAMP NOT NULL
);

-- Table: deposit_withdraw_history
CREATE TABLE IF NOT EXISTS deposit_withdraw_history (
    transaction_id     BIGINT PRIMARY KEY,
    account_number     BIGINT NOT NULL,
    amount             BIGINT NOT NULL,
    created_at         TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_number FOREIGN KEY (account_number) REFERENCES savings_account(account_number)
);
CREATE INDEX IF NOT EXISTS idx_udwh_account_number ON deposit_withdraw_history(account_number);
CREATE INDEX IF NOT EXISTS idx_udwh_created_at ON deposit_withdraw_history(created_at);

-- Table: money_transfer_history
CREATE TABLE IF NOT EXISTS money_transfer_history (
    transaction_id           BIGINT PRIMARY KEY,
    source_account_number    BIGINT NOT NULL,
    destination_account_number BIGINT NOT NULL,
    amount                  BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    CONSTRAINT fk_source_account FOREIGN KEY (source_account_number) REFERENCES savings_account(account_number),
    CONSTRAINT fk_destination_account FOREIGN KEY (destination_account_number) REFERENCES savings_account(account_number)
);
CREATE INDEX IF NOT EXISTS idx_mth_source_account ON money_transfer_history(source_account_number);
CREATE INDEX IF NOT EXISTS idx_mth_destination_account ON money_transfer_history(destination_account_number);
CREATE INDEX IF NOT EXISTS idx_mth_created_at ON money_transfer_history(created_at);
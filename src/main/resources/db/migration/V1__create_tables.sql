-- Merged Flyway migration: All tables and indexes (MySQL compatible)

-- Table: savings_account
CREATE TABLE IF NOT EXISTS savings_account (
  account_number BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  personal_id BIGINT NOT NULL,
  balance BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table: deposit_withdraw_history
CREATE TABLE IF NOT EXISTS deposit_withdraw_history (
    transaction_id     BIGINT PRIMARY KEY,
    account_number     BIGINT NOT NULL,
    amount             BIGINT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_number FOREIGN KEY (account_number) REFERENCES savings_account(account_number),
    INDEX idx_udwh_account_number (account_number),
    INDEX idx_udwh_created_at (created_at)
);

-- Table: money_transfer_history
CREATE TABLE IF NOT EXISTS money_transfer_history (
    transaction_id           BIGINT PRIMARY KEY,
    source_account_number    BIGINT NOT NULL,
    destination_account_number BIGINT NOT NULL,
    amount                  BIGINT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_account FOREIGN KEY (source_account_number) REFERENCES savings_account(account_number),
    CONSTRAINT fk_destination_account FOREIGN KEY (destination_account_number) REFERENCES savings_account(account_number),
    INDEX idx_mth_source_account (source_account_number),
    INDEX idx_mth_destination_account (destination_account_number),
    INDEX idx_mth_created_at (created_at)
);
-- Members of expense groups. Kept deliberately thin for now;
-- authentication fields arrive when Spring Security goes in.
CREATE TABLE app_user (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A set of people sharing expenses: a trip, a flat, a team.
CREATE TABLE expense_group (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Join table: which users belong to which group.
CREATE TABLE group_membership (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT      NOT NULL REFERENCES expense_group(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id)      ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);

-- One expense: a single person paid, to be shared among several.
CREATE TABLE expense (
    id            BIGSERIAL      PRIMARY KEY,
    group_id      BIGINT         NOT NULL REFERENCES expense_group(id) ON DELETE CASCADE,
    paid_by_id    BIGINT         NOT NULL REFERENCES app_user(id),
    description   VARCHAR(255)   NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    split_type    VARCHAR(20)    NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- One row per participant in an expense: their share of it.
-- The sum of shares for an expense must equal the expense amount;
-- enforced in the service layer, since SQL cannot express it cheaply.
CREATE TABLE expense_split (
    id          BIGSERIAL      PRIMARY KEY,
    expense_id  BIGINT         NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    user_id     BIGINT         NOT NULL REFERENCES app_user(id),
    share       NUMERIC(19, 2) NOT NULL CHECK (share >= 0),
    CONSTRAINT uq_expense_participant UNIQUE (expense_id, user_id)
);

-- Indexes for the queries this application actually runs.
CREATE INDEX idx_membership_group   ON group_membership (group_id);
CREATE INDEX idx_expense_group      ON expense (group_id);
CREATE INDEX idx_split_expense      ON expense_split (expense_id);
CREATE INDEX idx_split_user         ON expense_split (user_id);
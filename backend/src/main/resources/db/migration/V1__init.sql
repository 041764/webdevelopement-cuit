-- V1: minimal schema for tutor-management (SQLite)
-- Note: SQLite foreign keys are connection-scoped; enable via datasource connection-init-sql.

CREATE TABLE IF NOT EXISTS college (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE IF NOT EXISTS "user" (
  id INTEGER PRIMARY KEY,
  user_type TEXT NOT NULL CHECK (user_type IN ('STUDENT', 'TEACHER')),
  user_no TEXT NOT NULL,
  name TEXT NOT NULL,
  college_id INTEGER NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT uq_user_type_no UNIQUE (user_type, user_no),
  CONSTRAINT fk_user_college FOREIGN KEY (college_id) REFERENCES college(id)
);

CREATE INDEX IF NOT EXISTS idx_user_college_id ON "user"(college_id);

CREATE TABLE IF NOT EXISTS role (
  id INTEGER PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_role (
  user_id INTEGER NOT NULL,
  role_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS local_credential (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL UNIQUE,
  client_salt TEXT NOT NULL,
  client_hash TEXT NOT NULL,
  server_hash TEXT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_credential_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refresh_token (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  token_hash TEXT NOT NULL UNIQUE,
  device_id TEXT NULL,
  issued_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  revoked_at TEXT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_token(expires_at);

CREATE TABLE IF NOT EXISTS "class" (
  id INTEGER PRIMARY KEY,
  term TEXT NOT NULL CHECK (term GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-[12]'),
  name TEXT NOT NULL,
  college_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_class_college FOREIGN KEY (college_id) REFERENCES college(id)
);

CREATE INDEX IF NOT EXISTS idx_class_term ON "class"(term);
CREATE INDEX IF NOT EXISTS idx_class_college_id ON "class"(college_id);

CREATE TABLE IF NOT EXISTS class_student (
  id INTEGER PRIMARY KEY,
  class_id INTEGER NOT NULL,
  student_user_id INTEGER NOT NULL,
  joined_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT uq_class_student UNIQUE (class_id, student_user_id),
  CONSTRAINT fk_class_student_class FOREIGN KEY (class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_class_student_user FOREIGN KEY (student_user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_class_student_student ON class_student(student_user_id);

CREATE TABLE IF NOT EXISTS class_tutor (
  id INTEGER PRIMARY KEY,
  class_id INTEGER NOT NULL,
  tutor_user_id INTEGER NOT NULL,
  assigned_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT uq_class_tutor UNIQUE (class_id, tutor_user_id),
  CONSTRAINT fk_class_tutor_class FOREIGN KEY (class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_class_tutor_user FOREIGN KEY (tutor_user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_class_tutor_tutor ON class_tutor(tutor_user_id);

CREATE TABLE IF NOT EXISTS activity (
  id INTEGER PRIMARY KEY,
  class_id INTEGER NOT NULL,
  term TEXT NOT NULL CHECK (term GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-[12]'),
  title TEXT NOT NULL,
  description TEXT NULL,
  capacity INTEGER NULL CHECK (capacity IS NULL OR capacity > 0),
  requires_review INTEGER NOT NULL DEFAULT 0 CHECK (requires_review IN (0, 1)),
  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
  created_by_user_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_activity_class FOREIGN KEY (class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_activity_creator FOREIGN KEY (created_by_user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_activity_class_term_status ON activity(class_id, term, status);

CREATE TABLE IF NOT EXISTS activity_signup (
  id INTEGER PRIMARY KEY,
  activity_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('APPLIED', 'APPROVED', 'REJECTED', 'CANCELED')),
  note TEXT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  reviewed_at TEXT NULL,
  reviewed_by_user_id INTEGER NULL,
  CONSTRAINT uq_activity_signup UNIQUE (activity_id, user_id),
  CONSTRAINT fk_signup_activity FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
  CONSTRAINT fk_signup_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_signup_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_activity_signup_activity_status ON activity_signup(activity_id, status);
CREATE INDEX IF NOT EXISTS idx_activity_signup_user_id ON activity_signup(user_id);

CREATE TABLE IF NOT EXISTS plan (
  id INTEGER PRIMARY KEY,
  owner_type TEXT NOT NULL CHECK (owner_type IN ('CLASS', 'USER')),
  owner_user_id INTEGER NULL,
  owner_class_id INTEGER NULL,
  term TEXT NOT NULL CHECK (term GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-[12]'),
  title TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_plan_owner_user FOREIGN KEY (owner_user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_plan_owner_class FOREIGN KEY (owner_class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT ck_plan_owner_consistent CHECK (
    (owner_type = 'USER' AND owner_user_id IS NOT NULL AND owner_class_id IS NULL) OR
    (owner_type = 'CLASS' AND owner_class_id IS NOT NULL AND owner_user_id IS NULL)
  )
);

CREATE INDEX IF NOT EXISTS idx_plan_owner_user_term ON plan(owner_user_id, term);
CREATE INDEX IF NOT EXISTS idx_plan_owner_class_term ON plan(owner_class_id, term);

CREATE TABLE IF NOT EXISTS plan_item (
  id INTEGER PRIMARY KEY,
  plan_id INTEGER NOT NULL,
  title TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'todo' CHECK (status IN ('todo', 'doing', 'done')),
  sort_order INTEGER NOT NULL DEFAULT 0,
  due_date TEXT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  updated_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_plan_item_plan FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_plan_item_plan_status ON plan_item(plan_id, status);

CREATE TABLE IF NOT EXISTS plan_item_progress (
  id INTEGER PRIMARY KEY,
  plan_item_id INTEGER NOT NULL,
  percent INTEGER NOT NULL CHECK (percent BETWEEN 0 AND 100),
  note TEXT NULL,
  created_by_user_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_plan_item_progress_item FOREIGN KEY (plan_item_id) REFERENCES plan_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_plan_item_progress_creator FOREIGN KEY (created_by_user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_plan_item_progress_item_created_at ON plan_item_progress(plan_item_id, created_at);

CREATE TABLE IF NOT EXISTS evaluation (
  id INTEGER PRIMARY KEY,
  evaluator_user_id INTEGER NOT NULL,
  evaluatee_user_id INTEGER NOT NULL,
  term TEXT NOT NULL CHECK (term GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-[12]'),
  score_total INTEGER NOT NULL CHECK (score_total >= 0),
  comment TEXT NULL,
  created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT fk_eval_evaluator FOREIGN KEY (evaluator_user_id) REFERENCES "user"(id),
  CONSTRAINT fk_eval_evaluatee FOREIGN KEY (evaluatee_user_id) REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_eval_term ON evaluation(term);
CREATE INDEX IF NOT EXISTS idx_eval_evaluatee ON evaluation(evaluatee_user_id);

CREATE TABLE IF NOT EXISTS evaluation_detail (
  id INTEGER PRIMARY KEY,
  evaluation_id INTEGER NOT NULL,
  item_key TEXT NOT NULL,
  score INTEGER NOT NULL,
  comment TEXT NULL,
  CONSTRAINT fk_eval_detail_eval FOREIGN KEY (evaluation_id) REFERENCES evaluation(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_eval_detail_eval ON evaluation_detail(evaluation_id);

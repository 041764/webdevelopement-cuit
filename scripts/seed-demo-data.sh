#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

DB_PATH="${APP_DB_PATH:-$ROOT_DIR/backend/data/dev.db}"
PEPPER="${APP_PASSWORD_PEPPER:-dev-pepper}"
DEMO_TERM="${DEMO_TERM:-2026-02-23-1}"
COLLEGE_NAME="${COLLEGE_NAME:-计算机学院}"
CLASS_NAME="${CLASS_NAME:-计科 1 班}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: '$1' not found" >&2
    exit 1
  fi
}

sql_escape() {
  printf %s "$1" | sed "s/'/''/g"
}

sha256_b64url() {
  node -e "console.log(require('crypto').createHash('sha256').update(process.argv[1]).digest('base64url'))" "$1"
}

bcrypt_hash() {
  htpasswd -bnBC 10 "" "$1" | sed 's/^://' | tr -d '\n'
}

sql_exec() {
  sqlite3 "$DB_PATH" "$1"
}

sql_scalar() {
  sqlite3 -noheader -batch "$DB_PATH" "$1"
}

upsert_credential() {
  local user_id="$1"
  local user_type="$2"
  local user_no="$3"
  local password="$4"

  local client_salt client_hash combined server_hash
  client_salt="$(sha256_b64url "${user_type}:${user_no}")"
  client_hash="$(sha256_b64url "${client_salt}:${password}")"
  combined="${PEPPER}:${client_hash}"
  server_hash="$(bcrypt_hash "$combined")"

  sql_exec "DELETE FROM local_credential WHERE user_id=${user_id};"
  sql_exec "INSERT INTO local_credential(user_id, client_salt, client_hash, server_hash, created_at, updated_at) VALUES(${user_id}, '$(sql_escape "$client_salt")', '$(sql_escape "$client_hash")', '$(sql_escape "$server_hash")', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
}

require_cmd sqlite3
require_cmd node
require_cmd htpasswd

mkdir -p "$(dirname "$DB_PATH")"

echo "DB_PATH=$DB_PATH"
echo "DEMO_TERM=$DEMO_TERM"
echo "COLLEGE_NAME=$COLLEGE_NAME"
echo "CLASS_NAME=$CLASS_NAME"
echo "PEPPER=$PEPPER"
echo

echo "[1/5] Ensure schema exists"
sql_exec ".read $ROOT_DIR/backend/src/main/resources/db/migration/V1__init.sql"

echo "[2/5] Upsert college + roles"
sql_exec "INSERT OR IGNORE INTO college(name) VALUES('$(sql_escape "$COLLEGE_NAME")');"
college_id="$(sql_scalar "SELECT id FROM college WHERE name='$(sql_escape "$COLLEGE_NAME")' LIMIT 1;")"
if [[ -z "${college_id:-}" ]]; then
  echo "ERROR: failed to resolve college_id" >&2
  exit 1
fi

sql_exec "INSERT OR IGNORE INTO role(code, name) VALUES('ADMIN_SCHOOL','校级管理员');"
sql_exec "INSERT OR IGNORE INTO role(code, name) VALUES('ADMIN_COLLEGE','院级管理员');"
sql_exec "INSERT OR IGNORE INTO role(code, name) VALUES('TUTOR','导师');"
role_admin_school_id="$(sql_scalar "SELECT id FROM role WHERE code='ADMIN_SCHOOL' LIMIT 1;")"
role_admin_college_id="$(sql_scalar "SELECT id FROM role WHERE code='ADMIN_COLLEGE' LIMIT 1;")"
role_tutor_id="$(sql_scalar "SELECT id FROM role WHERE code='TUTOR' LIMIT 1;")"

echo "[3/5] Create users"
sql_exec "INSERT OR IGNORE INTO \"user\"(user_type, user_no, name, college_id, status) VALUES('TEACHER','4001','Admin School',NULL,'ACTIVE');"
sql_exec "INSERT OR IGNORE INTO \"user\"(user_type, user_no, name, college_id, status) VALUES('TEACHER','1100','Admin College',${college_id},'ACTIVE');"
sql_exec "INSERT OR IGNORE INTO \"user\"(user_type, user_no, name, college_id, status) VALUES('TEACHER','1001','Tutor A',${college_id},'ACTIVE');"

for no in 2001 2002 2003 2004 2005 2006 2007 2008 2009 2010; do
  sql_exec "INSERT OR IGNORE INTO \"user\"(user_type, user_no, name, college_id, status) VALUES('STUDENT','${no}','Student ${no#20}',${college_id},'ACTIVE');"
done

admin_school_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='TEACHER' AND user_no='4001' LIMIT 1;")"
admin_college_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='TEACHER' AND user_no='1100' LIMIT 1;")"
tutor_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='TEACHER' AND user_no='1001' LIMIT 1;")"
student1_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='STUDENT' AND user_no='2001' LIMIT 1;")"

echo "[4/5] Assign roles + credentials"
sql_exec "INSERT OR IGNORE INTO user_role(user_id, role_id) VALUES(${admin_school_user_id}, ${role_admin_school_id});"
sql_exec "INSERT OR IGNORE INTO user_role(user_id, role_id) VALUES(${admin_college_user_id}, ${role_admin_college_id});"
sql_exec "INSERT OR IGNORE INTO user_role(user_id, role_id) VALUES(${tutor_user_id}, ${role_tutor_id});"

upsert_credential "$admin_school_user_id" "TEACHER" "4001" "Admin@123"
upsert_credential "$admin_college_user_id" "TEACHER" "1100" "AdminC@123"
upsert_credential "$tutor_user_id" "TEACHER" "1001" "Teacher@123"

for no in 2001 2002 2003 2004 2005 2006 2007 2008 2009 2010; do
  uid="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='STUDENT' AND user_no='${no}' LIMIT 1;")"
  upsert_credential "$uid" "STUDENT" "$no" "Student@123"
done

echo "[5/5] Create class + demo data"
class_id="$(sql_scalar "SELECT id FROM \"class\" WHERE term='$(sql_escape "$DEMO_TERM")' AND name='$(sql_escape "$CLASS_NAME")' AND college_id=${college_id} LIMIT 1;")"
if [[ -z "${class_id:-}" ]]; then
  sql_exec "INSERT INTO \"class\"(term, name, college_id, created_at) VALUES('$(sql_escape "$DEMO_TERM")','$(sql_escape "$CLASS_NAME")',${college_id},CURRENT_TIMESTAMP);"
  class_id="$(sql_scalar "SELECT id FROM \"class\" WHERE term='$(sql_escape "$DEMO_TERM")' AND name='$(sql_escape "$CLASS_NAME")' AND college_id=${college_id} ORDER BY id DESC LIMIT 1;")"
fi

sql_exec "INSERT OR IGNORE INTO class_tutor(class_id, tutor_user_id, assigned_at) VALUES(${class_id}, ${tutor_user_id}, CURRENT_TIMESTAMP);"
for no in 2001 2002 2003 2004 2005 2006 2007 2008 2009 2010; do
  uid="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='STUDENT' AND user_no='${no}' LIMIT 1;")"
  sql_exec "INSERT OR IGNORE INTO class_student(class_id, student_user_id, joined_at) VALUES(${class_id}, ${uid}, CURRENT_TIMESTAMP);"
done

sql_exec "INSERT INTO activity(class_id, term, title, description, capacity, requires_review, status, created_by_user_id, created_at)
          VALUES(${class_id}, '$(sql_escape "$DEMO_TERM")', '迎新志愿活动', '演示：学生报名、导师审核', 30, 1, 'PUBLISHED', ${tutor_user_id}, CURRENT_TIMESTAMP);"
published_activity_id="$(sql_scalar "SELECT id FROM activity WHERE class_id=${class_id} AND term='$(sql_escape "$DEMO_TERM")' AND title='迎新志愿活动' ORDER BY id DESC LIMIT 1;")"

sql_exec "INSERT INTO activity(class_id, term, title, description, capacity, requires_review, status, created_by_user_id, created_at)
          VALUES(${class_id}, '$(sql_escape "$DEMO_TERM")', '班会签到活动', '演示：草稿/发布/关闭', NULL, 0, 'DRAFT', ${tutor_user_id}, CURRENT_TIMESTAMP);"

sql_exec "INSERT OR IGNORE INTO activity_signup(activity_id, user_id, status, note, created_at, reviewed_at, reviewed_by_user_id)
          VALUES(${published_activity_id}, ${student1_user_id}, 'APPLIED', '我想参加', CURRENT_TIMESTAMP, NULL, NULL);"
student2_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='STUDENT' AND user_no='2002' LIMIT 1;")"
sql_exec "INSERT OR IGNORE INTO activity_signup(activity_id, user_id, status, note, created_at, reviewed_at, reviewed_by_user_id)
          VALUES(${published_activity_id}, ${student2_user_id}, 'APPROVED', '已通过示例', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ${tutor_user_id});"
student3_user_id="$(sql_scalar "SELECT id FROM \"user\" WHERE user_type='STUDENT' AND user_no='2003' LIMIT 1;")"
sql_exec "INSERT OR IGNORE INTO activity_signup(activity_id, user_id, status, note, created_at, reviewed_at, reviewed_by_user_id)
          VALUES(${published_activity_id}, ${student3_user_id}, 'REJECTED', '名额有限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ${tutor_user_id});"

sql_exec "INSERT INTO plan(owner_type, owner_user_id, owner_class_id, term, title, created_at)
          VALUES('CLASS', NULL, ${class_id}, '$(sql_escape "$DEMO_TERM")', '班级周计划（演示）', CURRENT_TIMESTAMP);"
class_plan_id="$(sql_scalar "SELECT id FROM plan WHERE owner_type='CLASS' AND owner_class_id=${class_id} AND term='$(sql_escape "$DEMO_TERM")' ORDER BY id DESC LIMIT 1;")"

sql_exec "INSERT INTO plan_item(plan_id, title, status, sort_order, due_date, created_at, updated_at)
          VALUES(${class_plan_id}, '完成迎新活动报名', 'todo', 0, '2026-03-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
sql_exec "INSERT INTO plan_item(plan_id, title, status, sort_order, due_date, created_at, updated_at)
          VALUES(${class_plan_id}, '提交周总结', 'doing', 1, '2026-03-03', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
sql_exec "INSERT INTO plan_item(plan_id, title, status, sort_order, due_date, created_at, updated_at)
          VALUES(${class_plan_id}, '参加班会', 'done', 2, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"

doing_item_id="$(sql_scalar "SELECT id FROM plan_item WHERE plan_id=${class_plan_id} AND status='doing' ORDER BY id DESC LIMIT 1;")"
sql_exec "INSERT INTO plan_item_progress(plan_item_id, percent, note, created_by_user_id, created_at)
          VALUES(${doing_item_id}, 40, '进度演示：已完成 40%', ${tutor_user_id}, CURRENT_TIMESTAMP);"

sql_exec "INSERT INTO plan(owner_type, owner_user_id, owner_class_id, term, title, created_at)
          VALUES('USER', ${student1_user_id}, NULL, '$(sql_escape "$DEMO_TERM")', '个人学习计划（演示）', CURRENT_TIMESTAMP);"
user_plan_id="$(sql_scalar "SELECT id FROM plan WHERE owner_type='USER' AND owner_user_id=${student1_user_id} AND term='$(sql_escape "$DEMO_TERM")' ORDER BY id DESC LIMIT 1;")"

sql_exec "INSERT INTO plan_item(plan_id, title, status, sort_order, due_date, created_at, updated_at)
          VALUES(${user_plan_id}, '阅读课程资料', 'doing', 0, '2026-03-02', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"

sql_exec "INSERT INTO evaluation(evaluator_user_id, evaluatee_user_id, term, score_total, comment, created_at)
          VALUES(${tutor_user_id}, ${student1_user_id}, '$(sql_escape "$DEMO_TERM")', 85, '表现良好（演示）', CURRENT_TIMESTAMP);"
eval_id="$(sql_scalar "SELECT id FROM evaluation WHERE evaluator_user_id=${tutor_user_id} AND evaluatee_user_id=${student1_user_id} AND term='$(sql_escape "$DEMO_TERM")' ORDER BY id DESC LIMIT 1;")"

sql_exec "INSERT INTO evaluation_detail(evaluation_id, item_key, score, comment) VALUES(${eval_id}, 'attitude', 40, '态度积极');"
sql_exec "INSERT INTO evaluation_detail(evaluation_id, item_key, score, comment) VALUES(${eval_id}, 'attendance', 45, '出勤良好');"

echo
cat <<EOF
Seed completed.

Login accounts (userType + id + password):
  - Admin (school): TEACHER / 4001 / Admin@123
  - Admin (college): TEACHER / 1100 / AdminC@123
  - Tutor (teacher): TEACHER / 1001 / Teacher@123
  - Student: STUDENT / 2001 / Student@123  (and 2002..2010 same password)

Backend must run with APP_PASSWORD_PEPPER='${PEPPER}' for these passwords to work.
EOF

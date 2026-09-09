-- =====================================================================
-- SCHEMA PATCH — chay idempotent moi lan BE khoi dong (qua SqlScriptRunner).
--
-- Vi sao ton tai: hibernate.hbm2ddl.auto=update chi them duoc cot NOT NULL
-- vao bang RONG. Bang da co du lieu (vd payments da co giao dich cu) thi
-- "ALTER TABLE ... ADD COLUMN ... NOT NULL" (khong co DEFAULT) bi PostgreSQL
-- tu choi -> hbm2ddl bo qua trong im lang -> moi cau SELECT/INSERT payment
-- nem "column ... does not exist" = Internal server error khi bam Thanh toan.
--
-- Quy tac viet o day:
--   * Chi DDL, khong DML nghiep vu.
--   * Bat buoc idempotent (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS) vi chay
--     lai moi lan start.
--   * Cot NOT NULL them vao bang co du lieu PHAI kem DEFAULT de backfill dong cu.
-- =====================================================================

-- Payment.currency: moi giao dich luon la VND (BE tu set, khong nhan tu client).
-- DEFAULT 'VND' de backfill cac dong payment da ton tai truoc khi co cot nay.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS currency varchar(5) NOT NULL DEFAULT 'VND';

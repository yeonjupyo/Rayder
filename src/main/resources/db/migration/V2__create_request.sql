-- Mirrors the Prisma `Request` model, ported from PostgreSQL to MariaDB.
--
-- Mapping notes (Postgres -> MariaDB):
--   Int @id @default(autoincrement())      -> BIGINT AUTO_INCREMENT (kept wide; request logs grow fast)
--   String @db.Uuid @default(uuid())       -> CHAR(36), generated in the application layer (MariaDB has no
--                                              native UUID type; UUID() SQL function exists but app-side
--                                              generation keeps it consistent with JPA/MyBatis-managed inserts)
--   Json / Json?                           -> JSON (MariaDB alias for LONGTEXT with JSON validation)
--   DateTime @db.Timestamptz(6)             -> DATETIME(6) (MariaDB has no tz-aware timestamp type;
--                                              store UTC and convert at the edges)
--   @updatedAt                              -> ON UPDATE CURRENT_TIMESTAMP(6)
CREATE TABLE IF NOT EXISTS request (
	id            BIGINT       NOT NULL AUTO_INCREMENT,
	uuid          CHAR(36)     NOT NULL,
	ip            VARCHAR(64)  NOT NULL,
	user_agent    JSON         NOT NULL,
	method        VARCHAR(10)  NOT NULL,
	host          VARCHAR(255) NULL,
	url           VARCHAR(255) NULL,
	body          JSON         NULL,
	query         JSON         NULL,
	params        VARCHAR(1024) NULL,
	headers       VARCHAR(4096) NULL,
	cookies       VARCHAR(2048) NULL,
	response_body JSON         NULL,
	error         TEXT         NULL,
	status        INT          NULL,
	duration      INT          NULL,

	request_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	response_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	deleted_at  DATETIME(6) NULL,

	PRIMARY KEY (id),
	UNIQUE KEY uk_request_uuid (uuid),
	KEY idx_request_created_at (created_at),
	KEY idx_request_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

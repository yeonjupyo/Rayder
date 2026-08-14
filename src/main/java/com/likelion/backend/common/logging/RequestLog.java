package com.likelion.backend.common.logging;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Domain object for the `request` table (ported from the Prisma `Request`
 * model). JSON-typed columns ({@code userAgent}, {@code body}, {@code query},
 * {@code responseBody}) are held here as pre-serialized JSON strings; the
 * mapper binds them straight into MariaDB's native JSON column type, and the
 * DB itself validates well-formedness.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

	private Long id;
	private String uuid;
	private String ip;
	private String userAgent;
	private RequestMethod method;
	private String host;
	private String url;
	private String body;
	private String query;
	private String params;
	private String headers;
	private String cookies;
	private String responseBody;
	private String error;
	private Integer status;
	private Integer duration;

	private LocalDateTime requestAt;
	private LocalDateTime responseAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}

package com.likelion.backend.example.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sample domain entity showing the shape MyBatis result maps should bind to.
 * Field names line up 1:1 with the `example` table columns via MyBatis'
 * default camelCase<->snake_case mapping (see application.yml
 * `map-underscore-to-camel-case`).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Example {

	private Long id;
	private String name;
	private LocalDateTime createdAt;
}

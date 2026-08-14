package com.likelion.backend.example.controller;

import com.likelion.backend.example.domain.Example;
import com.likelion.backend.example.service.ExampleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExampleController {

	private final ExampleService exampleService;

	@GetMapping("/api/examples")
	public List<Example> findAll() {
		return exampleService.findAll();
	}

	@GetMapping("/api/examples/{id}")
	public Example findById(@PathVariable Long id) {
		return exampleService.findById(id);
	}

	@PostMapping("/api/examples")
	public Example create(@RequestParam String name) {
		return exampleService.create(name);
	}
}

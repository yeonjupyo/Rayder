package com.likelion.backend.example.service;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.example.domain.Example;
import com.likelion.backend.example.mapper.ExampleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {

	private final ExampleMapper exampleMapper;

	public List<Example> findAll() {
		return exampleMapper.findAll();
	}

	public Example findById(Long id) {
		return exampleMapper.findById(id)
			.orElseThrow(() -> new BusinessException(
				"EXAMPLE_NOT_FOUND", "Example not found: " + id, HttpStatus.NOT_FOUND
			));
	}

	@Transactional
	public Example create(String name) {
		Example example = Example.builder().name(name).build();
		exampleMapper.insert(example);
		return example;
	}
}

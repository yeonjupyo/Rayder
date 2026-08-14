package com.likelion.backend.example.mapper;

import com.likelion.backend.example.domain.Example;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper interface. Implementations are generated at runtime from
 * the matching XML in {@code src/main/resources/mapper/ExampleMapper.xml}.
 */
@Mapper
public interface ExampleMapper {

	Optional<Example> findById(Long id);

	List<Example> findAll();

	int insert(Example example);

	int update(Example example);

	int deleteById(Long id);
}

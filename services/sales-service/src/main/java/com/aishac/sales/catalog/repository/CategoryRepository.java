package com.aishac.sales.catalog.repository;

import com.aishac.sales.catalog.entity.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    /** {@code lower(name)}, a mesma expressão do índice único {@code uk_categories_name_lower} (RN-02). */
    @Query("select count(c) > 0 from Category c where lower(c.name) = lower(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("select count(c) > 0 from Category c where lower(c.name) = lower(:name) and c.id <> :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") UUID id);
}

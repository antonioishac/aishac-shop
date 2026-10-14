package com.aishac.sales.catalog.service.impl;

import static com.aishac.sales.catalog.CatalogFixtures.CATEGORY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aishac.sales.catalog.CatalogFixtures;
import com.aishac.sales.catalog.dto.CategoryResponse;
import com.aishac.sales.catalog.dto.CreateCategoryRequest;
import com.aishac.sales.catalog.dto.UpdateCategoryRequest;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.CategoryRepository;
import com.aishac.sales.exception.CommonErrorCode;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;

/** Regras de categoria (RN-01 a RN-04), sem Spring nem banco. */
class CategoryServiceImplTest {

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryServiceImpl service =
            new CategoryServiceImpl(repository, Mappers.getMapper(CatalogMapper.class));

    // CA-01
    @Test
    void shouldCreateActiveCategory() {
        given(repository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(new CreateCategoryRequest("Calçados", null));

        assertThat(response.active()).isTrue();
        assertThat(response.name()).isEqualTo("Calçados");
    }

    // CA-02
    @Test
    void shouldRejectDuplicatedNameAndTranslateUniqueRace() {
        // Given
        given(repository.existsByNameIgnoreCase("Calçados")).willReturn(true);

        // Then
        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Calçados", null)))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS);

        // Given: outro cadastro gravou o mesmo nome entre a checagem e o INSERT
        given(repository.existsByNameIgnoreCase("Calçados")).willReturn(false);
        given(repository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException(
                        "duplicate",
                        new ConstraintViolationException("duplicate", new SQLException(), "uk_categories_name_lower")));

        // Then
        assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Calçados", null)))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
    }

    @Test
    void shouldUpdateWithCurrentVersionOnly() {
        // Given
        given(repository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(repository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        // Then
        assertThat(service.update(CATEGORY_ID, new UpdateCategoryRequest("Sapatos", "Couro", 0L))
                        .name())
                .isEqualTo("Sapatos");
        assertThatThrownBy(() -> service.update(CATEGORY_ID, new UpdateCategoryRequest("Sapatos", null, 3L)))
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.CONCURRENT_MODIFICATION);
    }

    // CA-03
    @Test
    void shouldDeactivateAndActivate() {
        given(repository.findById(CATEGORY_ID)).willReturn(Optional.of(CatalogFixtures.category(true)));
        given(repository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.deactivate(CATEGORY_ID).active()).isFalse();
        assertThat(service.activate(CATEGORY_ID).active()).isTrue();
    }

    @Test
    void shouldAnswerNotFoundForUnknownCategory() {
        given(repository.findById(CATEGORY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(CATEGORY_ID))
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND);
    }
}

package com.aishac.sales.catalog.controller;

import com.aishac.sales.catalog.dto.CategoryPage;
import com.aishac.sales.catalog.dto.CategoryResponse;
import com.aishac.sales.catalog.dto.CreateCategoryRequest;
import com.aishac.sales.catalog.dto.UpdateCategoryRequest;
import com.aishac.sales.catalog.service.CategoryService;
import com.aishac.sales.dto.PageResponse;
import com.aishac.sales.dto.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Categories", description = "Categorias de produto (um nível)")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final CategoryService categoryService;

    @Operation(operationId = "createCategory", summary = "Cadastrar categoria", description = "Nasce ativa.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Categoria cadastrada",
                headers = @Header(name = "Location", description = "URL da categoria criada")),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou MALFORMED_REQUEST",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CATEGORY_NAME_ALREADY_EXISTS",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.created(location(response.id())).body(response);
    }

    @Operation(
            operationId = "listCategories",
            summary = "Listar categorias",
            description = "Paginado. Ordenação padrão: name,asc.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Página de categorias",
                content = @Content(schema = @Schema(implementation = CategoryPage.class))),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public PageResponse<CategoryResponse> list(
            @Parameter(description = "Parte do nome, sem diferenciar maiúsculas")
                    @RequestParam(required = false)
                    @Size(max = 80)
                    String name,
            @Parameter(description = "Situação") @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return PageResponse.of(categoryService.list(name, active, pageable));
    }

    @Operation(operationId = "getCategory", summary = "Buscar categoria por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoria"),
        @ApiResponse(
                responseCode = "404",
                description = "CATEGORY_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping("/{categoryId}")
    public CategoryResponse get(@PathVariable UUID categoryId) {
        return categoryService.findById(categoryId);
    }

    @Operation(
            operationId = "updateCategory",
            summary = "Atualizar categoria",
            description = "Substitui nome e descrição. Exige a version lida (lock otimista).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou MALFORMED_REQUEST",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CATEGORY_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CATEGORY_NAME_ALREADY_EXISTS ou CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    @Operation(
            operationId = "deactivateCategory",
            summary = "Desativar categoria",
            description = "Os produtos continuam ativos, mas deixam de ser vendáveis. Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoria desativada"),
        @ApiResponse(
                responseCode = "404",
                description = "CATEGORY_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{categoryId}/deactivate")
    public CategoryResponse deactivate(@PathVariable UUID categoryId) {
        return categoryService.deactivate(categoryId);
    }

    @Operation(operationId = "activateCategory", summary = "Reativar categoria", description = "Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoria ativa"),
        @ApiResponse(
                responseCode = "404",
                description = "CATEGORY_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{categoryId}/activate")
    public CategoryResponse activate(@PathVariable UUID categoryId) {
        return categoryService.activate(categoryId);
    }

    private static URI location(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}

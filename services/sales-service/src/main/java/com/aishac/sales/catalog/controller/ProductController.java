package com.aishac.sales.catalog.controller;

import com.aishac.sales.catalog.dto.CatalogSchemas;
import com.aishac.sales.catalog.dto.CreateProductRequest;
import com.aishac.sales.catalog.dto.ProductPage;
import com.aishac.sales.catalog.dto.ProductResponse;
import com.aishac.sales.catalog.dto.UpdateProductRequest;
import com.aishac.sales.catalog.service.ProductService;
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
import jakarta.validation.constraints.Pattern;
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

@Tag(name = "Products", description = "Produtos, preço e situação")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ProductService productService;

    @Operation(
            operationId = "createProduct",
            summary = "Cadastrar produto",
            description = "Nasce ativo, com o estoque inicial registrado como uma entrada no histórico.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Produto cadastrado",
                headers = @Header(name = "Location", description = "URL do produto criado")),
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
                description = "SKU_ALREADY_EXISTS",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CATEGORY_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.created(location(response.id())).body(response);
    }

    @Operation(
            operationId = "listProducts",
            summary = "Listar produtos",
            description = "Paginado. Filtros combináveis. sellable=true traz só produtos ativos de categorias ativas"
                    + " (vitrine). Ordenação por name, price ou createdAt; padrão createdAt,desc. Com o S3"
                    + " indisponível, a listagem funciona e primaryImage vem nulo.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Página de produtos (vazia se nenhum corresponder)",
                content = @Content(schema = @Schema(implementation = ProductPage.class))),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public PageResponse<ProductResponse> list(
            @Parameter(description = "Categoria") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Parte do nome, sem diferenciar maiúsculas")
                    @RequestParam(required = false)
                    @Size(max = 150)
                    String name,
            @Parameter(description = "Preço mínimo (inclusive)", schema = @Schema(ref = CatalogSchemas.MONEY))
                    @RequestParam(required = false)
                    @Pattern(regexp = CatalogSchemas.MONEY_PATTERN, message = CatalogSchemas.MONEY_MESSAGE)
                    String minPrice,
            @Parameter(description = "Preço máximo (inclusive)", schema = @Schema(ref = CatalogSchemas.MONEY))
                    @RequestParam(required = false)
                    @Pattern(regexp = CatalogSchemas.MONEY_PATTERN, message = CatalogSchemas.MONEY_MESSAGE)
                    String maxPrice,
            @Parameter(description = "Situação do produto") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Produto e categoria ativos") @RequestParam(required = false) Boolean sellable,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.of(productService.list(categoryId, name, minPrice, maxPrice, active, sellable, pageable));
    }

    @Operation(
            operationId = "getProduct",
            summary = "Buscar produto por id",
            description = "Qualquer situação. Inclui a imagem principal com URL pré-assinada (nula se o S3 falhar).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto"),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable UUID productId) {
        return productService.findById(productId);
    }

    @Operation(
            operationId = "updateProduct",
            summary = "Atualizar produto",
            description =
                    "Substitui nome, descrição, preço e categoria. SKU e estoque não mudam. Exige a version lida.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou MALFORMED_REQUEST",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND ou CATEGORY_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CATEGORY_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping("/{productId}")
    public ProductResponse update(@PathVariable UUID productId, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(productId, request);
    }

    @Operation(
            operationId = "deactivateProduct",
            summary = "Desativar produto",
            description = "Idempotente. Não existe exclusão de produto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto desativado"),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{productId}/deactivate")
    public ProductResponse deactivate(@PathVariable UUID productId) {
        return productService.deactivate(productId);
    }

    @Operation(operationId = "activateProduct", summary = "Reativar produto", description = "Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto ativo"),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{productId}/activate")
    public ProductResponse activate(@PathVariable UUID productId) {
        return productService.activate(productId);
    }

    private static URI location(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}

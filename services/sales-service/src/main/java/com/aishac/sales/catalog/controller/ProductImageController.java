package com.aishac.sales.catalog.controller;

import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.UpdateProductImageRequest;
import com.aishac.sales.catalog.service.ProductImageService;
import com.aishac.sales.dto.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Product images", description = "Imagens de produto no S3 (URL pré-assinada)")
@RestController
@RequestMapping("/api/v1/products/{productId}/images")
@RequiredArgsConstructor
public class ProductImageController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ProductImageService productImageService;

    @Operation(
            operationId = "uploadProductImage",
            summary = "Enviar imagem",
            description = "JPEG, PNG ou WebP (validado pelo conteúdo), até o limite configurado (padrão 5 MB), no"
                    + " máximo 8 por produto. A primeira imagem vira principal.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Imagem enviada",
                headers = @Header(name = "Location", description = "URL da imagem criada")),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR, INVALID_IMAGE_TYPE ou IMAGE_TOO_LARGE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "IMAGE_LIMIT_EXCEEDED",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "IMAGE_STORAGE_UNAVAILABLE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> upload(
            @PathVariable UUID productId,
            @Parameter(description = "JPEG, PNG ou WebP") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Texto alternativo (acessibilidade)") @RequestParam @NotBlank @Size(max = 150)
                    String altText) {
        ProductImageResponse response = productImageService.upload(productId, file, altText);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "listProductImages",
            summary = "Listar imagens do produto",
            description = "Ordenadas pela posição, com URL pré-assinada.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Imagens",
                content =
                        @Content(
                                mediaType = "application/json",
                                array =
                                        @ArraySchema(
                                                maxItems = 8,
                                                schema = @Schema(implementation = ProductImageResponse.class)))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "IMAGE_STORAGE_UNAVAILABLE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public List<ProductImageResponse> list(@PathVariable UUID productId) {
        return productImageService.list(productId);
    }

    @Operation(operationId = "getProductImage", summary = "Buscar imagem")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagem"),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND ou IMAGE_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "IMAGE_STORAGE_UNAVAILABLE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping("/{imageId}")
    public ProductImageResponse get(@PathVariable UUID productId, @PathVariable UUID imageId) {
        return productImageService.findById(productId, imageId);
    }

    @Operation(
            operationId = "updateProductImage",
            summary = "Atualizar metadados da imagem",
            description = "Texto alternativo, posição e/ou principal. Campos ausentes não mudam. O arquivo não muda.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagem atualizada"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou MALFORMED_REQUEST",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND ou IMAGE_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PatchMapping("/{imageId}")
    public ProductImageResponse update(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateProductImageRequest request) {
        return productImageService.update(productId, imageId, request);
    }

    @Operation(
            operationId = "deleteProductImage",
            summary = "Excluir imagem",
            description = "Banco primeiro, S3 depois. Se era a principal, a próxima pela posição assume.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Imagem excluída"),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND ou IMAGE_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId, @PathVariable UUID imageId) {
        productImageService.delete(productId, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "replaceProductImageFile",
            summary = "Substituir o arquivo da imagem",
            description = "Grava em chave nova e remove o objeto antigo. Mantém id, texto alternativo, posição e"
                    + " principal.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Arquivo substituído"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR, INVALID_IMAGE_TYPE ou IMAGE_TOO_LARGE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND ou IMAGE_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "IMAGE_STORAGE_UNAVAILABLE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping(path = "/{imageId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImageResponse replaceFile(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            @Parameter(description = "JPEG, PNG ou WebP") @RequestPart("file") MultipartFile file) {
        return productImageService.replaceFile(productId, imageId, file);
    }
}

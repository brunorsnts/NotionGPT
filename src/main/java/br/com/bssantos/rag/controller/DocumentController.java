package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.DocumentResponse;
import br.com.bssantos.rag.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> armazenaNovoDocumento(@RequestParam("file") MultipartFile file) {
        DocumentResponse response = documentService.salvaDocumento(file);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> retornaTodosOsDocumentos(
            @PageableDefault(size = 10, sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.retornaTodosOsDocumentos(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletaDocumento(@PathVariable String id) {
        documentService.deletaDocumento(id);
        return ResponseEntity.ok().build();
    }

}

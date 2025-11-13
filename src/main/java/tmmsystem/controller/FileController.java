package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tmmsystem.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService storage;
    public FileController(FileStorageService storage) { this.storage = storage; }

    @Operation(summary = "Serve stored file by filename")
    @GetMapping(value = "/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@Parameter(description = "Tên file") @PathVariable String filename) {
        try {
            byte[] bytes = storage.getFileByFilename(filename);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}


package tmmsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.storage.path:/data}")
    private String storagePath;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    /**
     * Upload contract file to local storage
     */
    public String uploadContractFile(MultipartFile file, Long contractId) throws IOException {
        validateFile(file);
        
        // Create directory structure: /data/contracts/{contractId}/
        Path contractDir = Paths.get(storagePath, "contracts", contractId.toString());
        Files.createDirectories(contractDir);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = "contract_" + contractId + "_" + System.currentTimeMillis() + extension;
        
        // Save file
        Path filePath = contractDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File uploaded successfully: {}", filePath);
        
        // Return relative path for database storage
        return "contracts/" + contractId + "/" + fileName;
    }
    
    /**
     * Get contract file URL (latest uploaded)
     */
    public String getContractFileUrl(Long contractId) {
        try {
            Path contractDir = Paths.get(storagePath, "contracts", contractId.toString());
            if (!Files.exists(contractDir)) {
                return null;
            }
            try (java.util.stream.Stream<Path> stream = Files.list(contractDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                        .map(path -> baseUrl + "/api/files/" + path.getFileName().toString())
                        .orElse(null);
            }
        } catch (IOException e) {
            log.error("Error getting contract file URL for contract ID: {}", contractId, e);
            return null;
        }
    }
    
    /**
     * Download contract file (latest uploaded)
     */
    public byte[] downloadContractFile(Long contractId) throws IOException {
        Path contractDir = Paths.get(storagePath, "contracts", contractId.toString());
        if (!Files.exists(contractDir)) {
            throw new IOException("Contract directory not found");
        }
        try (java.util.stream.Stream<Path> stream = Files.list(contractDir)) {
            Path filePath = stream
                    .filter(Files::isRegularFile)
                    .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .orElseThrow(() -> new IOException("Contract file not found"));
            return Files.readAllBytes(filePath);
        }
    }
    
    /**
     * Get contract file name for download (latest uploaded)
     */
    public String getContractFileName(Long contractId) throws IOException {
        Path contractDir = Paths.get(storagePath, "contracts", contractId.toString());
        if (!Files.exists(contractDir)) {
            throw new IOException("Contract directory not found");
        }
        try (java.util.stream.Stream<Path> stream = Files.list(contractDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .map(path -> path.getFileName().toString())
                    .orElseThrow(() -> new IOException("Contract file not found"));
        }
    }
    
    /**
     * Upload production order file
     */
    public String uploadProductionOrderFile(MultipartFile file, Long productionOrderId) throws IOException {
        validateFile(file);
        
        // Create directory structure: /data/production-orders/{productionOrderId}/
        Path poDir = Paths.get(storagePath, "production-orders", productionOrderId.toString());
        Files.createDirectories(poDir);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = "production_order_" + productionOrderId + "_" + System.currentTimeMillis() + extension;
        
        // Save file
        Path filePath = poDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Production order file uploaded successfully: {}", filePath);
        
        // Return relative path for database storage
        return "production-orders/" + productionOrderId + "/" + fileName;
    }
    
    /**
     * Get production order file URL
     */
    public String getProductionOrderFileUrl(Long productionOrderId) {
        try {
            Path poDir = Paths.get(storagePath, "production-orders", productionOrderId.toString());
            if (!Files.exists(poDir)) {
                return null;
            }
            try (java.util.stream.Stream<Path> stream = Files.list(poDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> baseUrl + "/api/files/" + path.getFileName().toString())
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            log.error("Error getting production order file URL for PO ID: {}", productionOrderId, e);
            return null;
        }
    }
    
    /**
     * Upload quotation file (PDF/Doc/Excel/Images)
     */
    public String uploadQuotationFile(MultipartFile file, Long quotationId) throws IOException {
        validateFile(file);
        Path dir = Paths.get(storagePath, "quotations", quotationId.toString());
        Files.createDirectories(dir);
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = "quotation_" + quotationId + "_" + System.currentTimeMillis() + extension;
        Path filePath = dir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Quotation file uploaded successfully: {}", filePath);
        return "quotations/" + quotationId + "/" + fileName;
    }

    /**
     * Get quotation file URL
     */
    public String getQuotationFileUrl(Long quotationId) {
        try {
            Path dir = Paths.get(storagePath, "quotations", quotationId.toString());
            if (!Files.exists(dir)) return null;
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                        .map(path -> baseUrl + "/api/files/" + path.getFileName().toString())
                        .orElse(null);
            }
        } catch (IOException e) {
            log.error("Error getting quotation file URL for ID {}", quotationId, e);
            return null;
        }
    }

    /**
     * Download quotation file
     */
    public byte[] downloadQuotationFile(Long quotationId) throws IOException {
        Path dir = Paths.get(storagePath, "quotations", quotationId.toString());
        if (!Files.exists(dir)) throw new IOException("Quotation directory not found");
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            Path filePath = stream
                    .filter(Files::isRegularFile)
                    .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .orElseThrow(() -> new IOException("Quotation file not found"));
            return Files.readAllBytes(filePath);
        }
    }

    /**
     * Get quotation file name for download
     */
    public String getQuotationFileName(Long quotationId) throws IOException {
        Path dir = Paths.get(storagePath, "quotations", quotationId.toString());
        if (!Files.exists(dir)) throw new IOException("Quotation directory not found");
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .min((a,b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .map(p -> p.getFileName().toString())
                    .orElseThrow(() -> new IOException("Quotation file not found"));
        }
    }

    /**
     * Get file by filename (for API endpoint)
     */
    public byte[] getFileByFilename(String filename) throws IOException {
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            throw new IOException("Storage directory not found");
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(storageDir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .map(path -> {
                        try {
                            return Files.readAllBytes(path);
                        } catch (IOException e) {
                            log.error("Error reading file: {}", path, e);
                            return null;
                        }
                    })
                    .orElseThrow(() -> new IOException("File not found: " + filename));
        }
    }
    
    /**
     * Validate uploaded file - Allow images + PDF + Word + Excel
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        
        // Allow common types
        String contentType = file.getContentType();
        String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
        java.util.Set<String> allowedTypes = Set.of(
                // images
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
                // pdf/doc/xls
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        java.util.Set<String> allowedExt = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx");
        boolean ok = (contentType != null && allowedTypes.contains(contentType)) || allowedExt.contains(ext);
        if (!ok) {
            throw new IllegalArgumentException("Only image/PDF/Word/Excel files are allowed");
        }
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

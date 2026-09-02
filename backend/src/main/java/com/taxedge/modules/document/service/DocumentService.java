package com.taxedge.modules.document.service;

import com.taxedge.core.config.StorageConfig;
import com.taxedge.core.exception.ApiException;
import com.taxedge.modules.document.entity.Document;
import com.taxedge.modules.document.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repo;
    private final StorageConfig storage;

    public DocumentService(DocumentRepository repo, StorageConfig storage) {
        this.repo = repo; this.storage = storage;
    }

    public Document upload(Long userId, MultipartFile file, String category, String remarks) {
        if (file == null || file.isEmpty()) throw new ApiException("File is required");
        try {
            String orig = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String storedName = UUID.randomUUID() + "_" + orig.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = storage.getRoot().resolve(storedName);
            Files.copy(file.getInputStream(), target);
            Document d = Document.builder()
                    .userId(userId).fileName(storedName).originalName(orig)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .size(file.getSize()).storagePath(target.toString())
                    .category(category == null ? "OTHER" : category)
                    .remarks(remarks).build();
            return repo.save(d);
        } catch (IOException e) {
            throw new ApiException("Failed to store file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Document> listMine(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<Document> listAll() { return repo.findAll(); }

    public Document get(Long id, Long userId, boolean isAdmin) {
        Document d = repo.findById(id).orElseThrow(() -> new ApiException("Document not found", HttpStatus.NOT_FOUND));
        if (!isAdmin && !d.getUserId().equals(userId)) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return d;
    }

    public Path resolvePath(Document d) { return Path.of(d.getStoragePath()); }

    public void delete(Long id, Long userId, boolean isAdmin) {
        Document d = get(id, userId, isAdmin);
        try { Files.deleteIfExists(Path.of(d.getStoragePath())); } catch (IOException ignored) {}
        repo.delete(d);
    }
}

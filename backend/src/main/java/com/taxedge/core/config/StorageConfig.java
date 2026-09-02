package com.taxedge.core.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
public class StorageConfig {

    @Value("${app.storage.location}")
    private String location;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        this.root = Paths.get(location).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }
}

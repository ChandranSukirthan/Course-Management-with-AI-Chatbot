package com.duinophile.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    private static final String UPLOADS_DIR = "./uploads";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadDir = Paths.get(UPLOADS_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // Use toUri() to get a correct cross-platform file URI
            String resourceLocation = uploadDir.toAbsolutePath().toUri().toString();
            if (!resourceLocation.endsWith("/")) resourceLocation += "/";

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(resourceLocation);

            // Ensure the avatars sub-directory exists on startup
            java.nio.file.Path avatarDir = Paths.get(UPLOADS_DIR).resolve("avatars");
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
            }

            System.out.println("[FileUploadConfig] Serving uploads from: " + resourceLocation);
        } catch (IOException e) {
            System.err.println("[FileUploadConfig] Could not create uploads directory: " + e.getMessage());
        }
    }
}

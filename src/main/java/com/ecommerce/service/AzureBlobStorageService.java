package com.ecommerce.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AzureBlobStorageService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public AzureBlobStorageService(BlobServiceClient blobServiceClient,
                                   @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // Secure container instance fetching
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }

        // Generate a cryptographically unique filename to prevent row collision overwrites
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueBlobName = UUID.randomUUID().toString() + extension;

        // Open binary stream and upload straight to Azure Cloud
        BlobClient blobClient = containerClient.getBlobClient(uniqueBlobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Return the persistent public tracking URL string to save into PostgreSQL
        return blobClient.getBlobUrl();
    }
}
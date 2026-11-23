package com.example.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!isValidImageSignature(bytes)) {
                throw new IllegalArgumentException("Invalid image file content");
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "ecommerce/products",
                            "resource_type", "image"
                    ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    private boolean isValidImageSignature(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        // JPEG: FF D8 FF
        boolean isJpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;

        // WEBP: RIFF....WEBP
        boolean isWebp = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';

        return isJpeg || isPng || isWebp;
    }
}
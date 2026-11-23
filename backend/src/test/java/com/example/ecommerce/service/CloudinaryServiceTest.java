package com.example.ecommerce.service;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @Test
    void shouldRejectNonImageContentType() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "malware.txt", "text/plain",
                "hello world".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> cloudinaryService.uploadImage(txtFile));
    }

    @Test
    void shouldRejectSpoofedImageMagicBytes() {
        MockMultipartFile fakeJpg = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg",
                "NOT_A_REAL_JPEG_CONTENT".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cloudinaryService.uploadImage(fakeJpg));
        assertTrue(ex.getMessage().contains("Invalid image"));
    }
}
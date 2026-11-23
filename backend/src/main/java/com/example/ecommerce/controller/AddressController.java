package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.dto.AddressResponse;
import com.example.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getUserAddresses(Authentication authentication) {
        return ResponseEntity.ok(addressService.getUserAddresses(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(addressService.createAddress(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(addressService.updateAddress(authentication.getName(), id, request));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(addressService.setDefaultAddress(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {
        addressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddressRequest;
import com.example.ecommerce.dto.AddressResponse;
import com.example.ecommerce.entity.Address;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.AddressRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userEmail) {
        User user = getUser(userEmail);
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(String userEmail, AddressRequest request) {
        User user = getUser(userEmail);
        List<Address> existing = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());

        boolean shouldBeDefault = request.isDefault() || existing.isEmpty();
        if (shouldBeDefault) {
            addressRepository.clearDefaultForUser(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .label(request.label().trim())
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .streetAddress(request.streetAddress().trim())
                .city(request.city().trim())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country().trim())
                .isDefault(shouldBeDefault)
                .build();

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(String userEmail, Long id, AddressRequest request) {
        User user = getUser(userEmail);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(user.getId());
            address.setDefault(true);
        }

        address.setLabel(request.label().trim());
        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setStreetAddress(request.streetAddress().trim());
        address.setCity(request.city().trim());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country().trim());

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefaultAddress(String userEmail, Long id) {
        User user = getUser(userEmail);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        addressRepository.clearDefaultForUser(user.getId());
        address.setDefault(true);
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String userEmail, Long id) {
        User user = getUser(userEmail);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        addressRepository.delete(address);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getLabel(),
                a.getFullName(),
                a.getPhone(),
                a.getStreetAddress(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountry(),
                a.isDefault(),
                a.getCreatedAt()
        );
    }
}
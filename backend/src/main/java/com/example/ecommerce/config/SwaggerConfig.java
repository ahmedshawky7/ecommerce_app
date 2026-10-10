package com.example.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "E-Commerce API",
        version = "1.0.0",
        description = "Complete E-Commerce Backend API - Products, Cart, Orders, Payments, Admin Dashboard",
        contact = @Contact(
            name = "Ahmed Eltabakh",
            email = "ahmedeltabakh703@gmail.com"
        )
    ),
        servers = {
        @Server(url = "https://ecommerceapp-production-5c76.up.railway.app", 
                description = "Production Server"),
        @Server(url = "http://localhost:8080", 
                description = "Local Development Server")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class SwaggerConfig {
}
package com.inkloom.compliance.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplianceRequest(
        @NotBlank(message = "Please paste the script you want checked.")
        @Size(max = 20000, message = "That script is too long (max 20,000 characters).")
        String content,
        String productName,
        String category) {
}

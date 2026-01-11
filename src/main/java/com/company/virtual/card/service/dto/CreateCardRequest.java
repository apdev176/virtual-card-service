package com.company.virtual.card.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateCardRequest {
    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    @Digits(integer = 19, fraction = 2, message = "Only 2 decimal places allowed")
    private BigDecimal initialBalance;
}
package com.company.virtual.card.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCardRequest {
    
    @NotBlank(message = "Cardholder name is required")
    @JsonProperty("cardholderName")
    private String cardholderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    @Digits(integer = 19, fraction = 2, message = "Only 2 decimal places allowed")
    @JsonProperty("initialBalance")
    private BigDecimal initialBalance;
}
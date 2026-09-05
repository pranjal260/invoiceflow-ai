package com.invoiceflow.ai.dto.invoiceitem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.001") @Digits(integer = 16, fraction = 3) BigDecimal quantity,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
) {
}

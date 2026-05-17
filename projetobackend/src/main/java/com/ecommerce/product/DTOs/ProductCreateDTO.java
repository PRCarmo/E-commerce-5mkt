package com.ecommerce.product.DTOs;

import com.ecommerce.enums.ProductStatus;
import com.ecommerce.user.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductCreateDTO {
 
    @NotBlank
    @Size(max = 30)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer stock;

    @NotNull
    private User seller;

    @NotNull
    private ProductStatus status;
}
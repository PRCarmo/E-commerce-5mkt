package com.ecommerce.product.DTOs;

import com.ecommerce.enums.ProductStatus;
import com.ecommerce.user.model.User;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {

    @Size(max = 30)
    private String name;

    @Size(max = 255)
    private String description;

    private BigDecimal price;

    private Integer stock;

    private User seller;

    private ProductStatus status;
}
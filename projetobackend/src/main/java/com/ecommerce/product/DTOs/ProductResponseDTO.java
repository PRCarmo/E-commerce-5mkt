package com.ecommerce.product.DTOs;

import com.ecommerce.enums.ProductStatus;
import com.ecommerce.user.model.User;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponseDTO {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private User seller;
    private ProductStatus status;

}
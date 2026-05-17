-- Função de trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(180) NOT NULL UNIQUE,
    password    VARCHAR(72)  NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'SELLER', 'CUSTOMER')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- products
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    seller_id   BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name        VARCHAR(180)  NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock       INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_seller ON products (seller_id);
CREATE INDEX idx_products_status ON products (status);

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- carts
-- -----------------------------------------------------------------------------
CREATE TABLE carts (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT    NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- cart_items
-- -----------------------------------------------------------------------------
CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT    NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id  BIGINT    NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity    INTEGER   NOT NULL CHECK (quantity > 0),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

CREATE TRIGGER trg_cart_items_updated_at
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- orders
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    seller_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status        VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount  NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_seller   ON orders (seller_id);
CREATE INDEX idx_orders_status   ON orders (status);

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- -----------------------------------------------------------------------------
-- order_items  (imutável — sem updated_at)
-- -----------------------------------------------------------------------------
CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT        NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity    INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

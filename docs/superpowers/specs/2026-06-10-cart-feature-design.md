# Feature: Cart (carrinho persistente)

**Data:** 2026-06-10
**Status:** Aprovado para implementação
**Fase:** 8

## Contexto

As tabelas `carts` e `cart_items` já existem (V1) e as entidades `Cart`/`CartItem`
já mapeiam, mas não há service nem controller — o carrinho está "adormecido".
Hoje o pedido é criado direto em `POST /api/v1/orders` com a lista de itens.

Esta feature ativa o carrinho **sem nova migration**, mantendo o `POST /orders`
intacto. O checkout do carrinho **delega ao `OrderService.create` existente**,
reaproveitando a lógica já testada de validação de estoque e snapshot de preço.

## Decisões de domínio

- **Carrinho por cliente:** `Cart` é `@OneToOne` com `User` (constraint `UNIQUE` em
  `customer_id`). Criado lazy no primeiro acesso.
- **Só CUSTOMER** opera o carrinho. `SELLER`/`ADMIN` → 403 (autorização no service).
- **Add soma quantidade:** `POST /cart/items` com produto já presente incrementa a
  quantidade (respeita a constraint `UNIQUE (cart_id, product_id)`).
- **`PUT` define quantidade absoluta.**
- **Estoque não é reservado no carrinho** (é volátil). No add/update valida apenas
  que o produto existe e está `ACTIVE`. A validação real de estoque ocorre no
  checkout, dentro do `OrderService.create`.
- **`totalAmount` do carrinho é calculado dos preços atuais** dos produtos (o
  snapshot de preço só acontece no momento do pedido, em `OrderItem.unitPrice`).
- **Pedido simples:** o checkout cria UMA `Order` (com itens de N vendedores),
  coerente com a regra atual do projeto.

## Endpoints (`/api/v1/cart`, todos CUSTOMER)

| Método | Rota | Ação | Status sucesso |
|---|---|---|---|
| GET | `/api/v1/cart` | Retorna o carrinho (cria vazio se não existir) | 200 |
| POST | `/api/v1/cart/items` | Adiciona `{productId, quantity}`; se existe, soma | 200 |
| PUT | `/api/v1/cart/items/{productId}` | Define quantidade absoluta `{quantity}` | 200 |
| DELETE | `/api/v1/cart/items/{productId}` | Remove o item | 204 |
| DELETE | `/api/v1/cart` | Esvazia o carrinho | 204 |
| POST | `/api/v1/cart/checkout` | Cria a Order a partir do carrinho e esvazia | 201 (`OrderResponse`) |

### Erros
- Produto inexistente → 404 (`EntityNotFoundException`).
- Produto não `ACTIVE` no add/update → 400 (`InvalidOrderException`).
- Item inexistente no update/remove → 404.
- Não-CUSTOMER → 403 (`ForbiddenException`).
- Checkout com carrinho vazio → 400 (`InvalidOrderException`).
- Estoque insuficiente no checkout → 409 (`InsufficientStockException`, via OrderService).

## Componentes

### Repositórios
- `CartRepository extends JpaRepository<Cart, Long>`
  - `@EntityGraph(attributePaths = {"items", "items.product"}) Optional<Cart> findByCustomerId(Long customerId)`
- `CartItemRepository extends JpaRepository<CartItem, Long>` (para remoção/lookup pontual)

### Service: `CartService`
Depende de `CartRepository`, `ProductRepository`, `OrderService`.
- `Cart getOrCreateCart(User)` — busca ou cria carrinho do cliente.
- `Cart getCart(User)` — leitura (autoriza CUSTOMER).
- `Cart addItem(User, AddCartItemRequest)` — valida produto ACTIVE; soma quantidade ou cria item.
- `Cart updateItem(User, Long productId, UpdateCartItemRequest)` — define quantidade.
- `Cart removeItem(User, Long productId)`.
- `void clear(User)`.
- `Order checkout(User)` `@Transactional` — monta `OrderCreateRequest` dos itens,
  chama `orderService.create(req, user)`, esvazia o carrinho, retorna a Order.
- `private void ensureCustomer(User)` — lança `ForbiddenException` se role != CUSTOMER.

### Controller: `CartController`
`@AuthenticationPrincipal UserDetailsImpl`, `ResponseEntity`, conversão via `CartResponse.from(...)`.

### DTOs (`@Data`, padrão do projeto — não records)
- `AddCartItemRequest { @NotNull Long productId; @NotNull @Min(1) Integer quantity; }`
- `UpdateCartItemRequest { @NotNull @Min(1) Integer quantity; }`
- `CartItemResponse { Long productId; String productName; BigDecimal unitPrice; Integer quantity; BigDecimal subtotal; }`
- `CartResponse { Long id; Long customerId; List<CartItemResponse> items; BigDecimal totalAmount; LocalDateTime createdAt; LocalDateTime updatedAt; static from(Cart); }`

## Dependência entre features
`cart → order` (checkout chama `OrderService`) e `cart → product` (leitura).
`order` e `product` não dependem de `cart` → sem ciclo.

## Testes
- `CartServiceTest` (Mockito): autorização, add (novo/soma), update, remove, clear,
  produto inativo/inexistente, checkout (sucesso + carrinho vazio), delegação ao OrderService.
- `CartControllerTest` (`@WebMvcTest` + `@Import({SecurityConfig, WebSecurityTestConfig})`):
  status codes, validação (`@Valid`), 403 não-CUSTOMER, rotas.

## Fora de escopo
- Reserva de estoque no carrinho.
- Múltiplos carrinhos por usuário.
- Cupons/descontos.

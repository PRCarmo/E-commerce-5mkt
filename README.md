# 5MKT — Marketplace Hub

API REST de marketplace desenvolvida em Java + Spring Boot. Permite cadastro de vendedores e clientes, gestão de produtos por vendedor, e fluxo de pedidos com controle de estoque transacional.

Projeto acadêmico — disciplina de Programação Web-Backend, Universidade Tiradentes.

## Sumário

- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Endpoints principais](#endpoints-principais)
- [Exemplos de uso (curl)](#exemplos-de-uso-curl)
- [Decisões de arquitetura](#decisões-de-arquitetura)
- [Modelo de dados](#modelo-de-dados)
- [Mapeamento POO](#mapeamento-poo--o-que-aparece-no-código)
- [Estrutura de pacotes](#estrutura-de-pacotes)
- [Débitos técnicos conhecidos](#débitos-técnicos-conhecidos)
- [Autores](#autores)

## Stack

- Java 21
- Spring Boot 4.0.5 (webmvc, security, data-jpa, validation)
- PostgreSQL 16
- Flyway (migrations em `db/migration`)
- JWT (JJWT 0.12.6, HS384)
- Lombok
- Springdoc OpenAPI (Swagger UI)
- Maven, Docker Compose

## Pré-requisitos

- Java 21 (Temurin recomendado)
- Docker + Docker Compose
- Maven 3.9+ (ou usa o wrapper `./mvnw` incluso)

Verificar Java:

```bash
java -version
# openjdk version "21.x.x"
```

## Como rodar

**1. Subir o banco:**

Na raiz do repositório:

```bash
docker compose up -d postgres
```

Espera 2-3 segundos pro Postgres terminar de subir. Pra conferir:

```bash
docker compose ps
```

Deve mostrar `e-commerce-5mkt-postgres-1` com status `Up`.

**2. Rodar a aplicação:**

```bash
cd projetobackend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Flyway aplica as 3 migrations automaticamente (V1 cria schema, V2 alinha orders ao MER, V3 seeda o admin).

Aguardar a linha:

```
Started ProjetobackendApplication in X seconds
```

**3. Acessar o Swagger UI:**

http://localhost:8080/swagger-ui/index.html

**4. Derrubar:**

`Ctrl+C` no terminal da aplicação. Pra parar o Postgres:

```bash
docker compose down
```

Pra apagar o volume do banco também (resetar tudo):

```bash
docker compose down -v
```

## Endpoints principais

| Método | Endpoint | Acesso |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Público (CUSTOMER ou SELLER) |
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/users/me` | Autenticado |
| `PUT` | `/api/v1/users/me` | Autenticado |
| `GET` | `/api/v1/users` | ADMIN |
| `GET` | `/api/v1/users/{id}` | ADMIN |
| `GET` | `/api/v1/products` | Público (paginado, filtros) |
| `GET` | `/api/v1/products/{id}` | Público |
| `POST` | `/api/v1/products` | SELLER |
| `PUT` | `/api/v1/products/{id}` | Dono OU ADMIN |
| `DELETE` | `/api/v1/products/{id}` | Dono OU ADMIN |
| `POST` | `/api/v1/orders` | CUSTOMER |
| `GET` | `/api/v1/orders` | CUSTOMER (próprios) ou ADMIN (todos) |
| `GET` | `/api/v1/orders/{id}` | Dono OU ADMIN |
| `POST` | `/api/v1/orders/{id}/cancel` | Dono OU ADMIN, status=PENDING |

### Credenciais do ADMIN (seedadas pelo V3)

- **Email:** `admin@mkt5.com`
- **Senha:** `admin12345`

## Exemplos de uso (curl)

Cole no terminal com a aplicação rodando.

**Cadastrar um cliente:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana","email":"ana@test.com","password":"senha12345","role":"CUSTOMER"}'
```

**Login (guarda o token na variável):**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"senha12345"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo $TOKEN
```

**Ver os próprios dados:**

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
```

**Cadastrar um vendedor e criar um produto:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Loja XYZ","email":"loja@test.com","password":"senha12345","role":"SELLER"}'

TOKEN_SELLER=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"loja@test.com","password":"senha12345"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN_SELLER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Camiseta","description":"100% algodão","price":49.90,"stock":10}'
```

**Listar produtos (público, sem token):**

```bash
curl http://localhost:8080/api/v1/products
```

**Criar pedido (cliente compra 2 unidades do produto id=1):**

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

**Cancelar pedido (restaura estoque automaticamente):**

```bash
curl -X POST http://localhost:8080/api/v1/orders/1/cancel \
  -H "Authorization: Bearer $TOKEN"
```

## Decisões de arquitetura

### API REST monolítica em camadas

Cada feature segue o padrão `Controller → Service → Repository → Entity`. Pacotes organizados **por feature** (não por camada), facilitando navegação:

```
com.ecommerce.projetobackend.product
├── ProductController.java
├── ProductService.java
├── ProductRepository.java
├── Product.java
└── ProductCreateRequest.java, ProductResponse.java, ...
```

### Autenticação stateless via JWT

Token assinado com HS384 (selecionado automaticamente pelo tamanho da chave), enviado no header `Authorization: Bearer <token>`. Filtro `JwtAuthFilter` intercepta cada request, valida o token e popula o `SecurityContext`. Sem sessão no servidor — escala horizontalmente.

### Autorização no service, não com `@PreAuthorize`

Decisão de simplificar: a verificação de permissão (role + ownership) fica explícita no service, em `if`s claros. Mais legível pra estudantes acompanhando o código do que annotations.

Exemplo em `ProductService.update`:

```java
if (user.getRole() == UserRole.ADMIN) {
    return; // admin pode tudo
}
if (!product.getSeller().getId().equals(user.getId())) {
    throw new ForbiddenException("...");
}
```

### Controle de estoque transacional

A criação de pedido faz N operações correlacionadas: valida estoque de cada item, decrementa, cria o pedido e seus itens. Tudo dentro de `@Transactional` — se qualquer item falhar (estoque insuficiente, produto inativo), Spring reverte todos os saves. Mesma lógica no cancelamento: restaura estoque de N produtos atomicamente.

### Snapshot de preço em OrderItem

Quando um pedido é criado, `OrderItem.unitPrice` guarda o preço do produto **no momento da compra**. Se o vendedor alterar o preço depois, o item do pedido **não muda**. Comportamento esperado de qualquer e-commerce — histórico imutável.

### Timestamps no banco, via trigger

`created_at` e `updated_at` em todas as tabelas relevantes são preenchidos pelo Postgres (DEFAULT NOW() no INSERT + trigger `set_updated_at` no UPDATE). A aplicação não cuida disso. Vantagem: relógio único, sem drift entre instâncias.

Trade-off: como `insertable=false` nas entities JPA, após `save()` o entity em memória vem com timestamps `null`. Resolvido com `entityManager.refresh()` ou re-fetch via query dedicada (`findByIdWithDetails`).

### Migrations Flyway, schema imutável

V1 cria o schema inicial e nunca é alterado. Mudanças posteriores entram como V2, V3, etc. Garante que qualquer ambiente (dev, prod, CI) chega ao mesmo estado.

- **V1** — Schema inicial (6 tabelas: users, products, carts, cart_items, orders, order_items)
- **V2** — Remove `orders.seller_id` (alinhamento com decisão de pedido simples sem split por vendedor)
- **V3** — Seed do usuário ADMIN

### Tratamento global de erros

`@RestControllerAdvice` em `GlobalExceptionHandler` mapeia 10 tipos de exception pra responses padronizadas (`ErrorResponseDTO`). Cliente sempre recebe JSON consistente com `timestamp`, `status`, `error`, `message`, `path`. Validações de payload (`@Valid`) entram com lista de erros campo a campo.

### Anti user enumeration no login

`BadCredentialsException` (senha errada) e `UsernameNotFoundException` (email inexistente) retornam **resposta byte-a-byte idêntica** (401 + "Invalid credentials"). Atacante não consegue descobrir quais emails têm conta no sistema.

## Modelo de dados

```
┌─────────────┐         ┌─────────────┐
│    User     │ 1     N │   Product   │
│             ├─────────┤             │
│ id          │  vende  │ id          │
│ name        │         │ name        │
│ email       │         │ description │
│ password    │         │ price       │
│ role        │         │ stock       │
│ created_at  │         │ status      │
│ updated_at  │         │ seller_id   │
└──────┬──────┘         │ created_at  │
       │                │ updated_at  │
       │ 1              └──────┬──────┘
       │ faz                   │ 1
       │                       │ consta em
       │ N                     │ N
┌──────┴──────┐         ┌──────┴──────┐
│   Order     │ 1     N │ OrderItem   │
│             ├─────────┤             │
│ id          │  possui │ id          │
│ customer_id │         │ order_id    │
│ status      │         │ product_id  │
│ total_amount│         │ quantity    │
│ created_at  │         │ unit_price  │
│ updated_at  │         │ created_at  │
└─────────────┘         └─────────────┘
```

**Relações:**
- `User 1:N Product` (vende) — cada produto pertence a um vendedor
- `User 1:N Order` (faz) — cada pedido pertence a um cliente
- `Order 1:N OrderItem` (possui) — composição, cascade ALL
- `Product 1:N OrderItem` (consta em) — referência, sem cascade

**Tabelas adicionais no schema (não usadas no escopo atual):**
- `carts` e `cart_items` — preparadas pra futura implementação de carrinho persistente

## Mapeamento POO — o que aparece no código

Conceitos da disciplina aplicados ao projeto:

| Conceito | Onde aparece |
|---|---|
| **Encapsulamento** | Entidades com atributos `private`, acesso via getters/setters gerados pelo Lombok. DTOs separados das entities ocultam dados sensíveis (`password` nunca vaza). |
| **Herança** | `UserDetailsImpl implements UserDetails` (Spring Security), `JwtAuthFilter extends OncePerRequestFilter`, `UserRepository extends JpaRepository`. |
| **Polimorfismo (subtipagem)** | Spring injeta a interface `UserDetailsService` resolvendo em runtime para a implementação `UserDetailsServiceImpl` — late binding na prática. |
| **Polimorfismo paramétrico (generics)** | `JpaRepository<User, Long>`, `Optional<User>`, `Page<Product>`, `List<OrderItem>`. |
| **Tipo Abstrato de Dado (TAD)** | Cada entity é um TAD: atributos privados + comportamento exposto (`Order.getStatus()`, métodos de domínio futuros como `cancel()`). |
| **Tratamento de exceção** | Hierarquia de exceptions de domínio (`EntityNotFoundException`, `ForbiddenException`, `InvalidOrderException`, etc.) capturadas por handlers globais. |
| **Modelagem MER → Lógico → Físico** | MER conceitual → entities JPA com `@ManyToOne`/`@OneToMany` → `V1__initial_schema.sql` físico no Postgres. |

## Estrutura de pacotes

```
src/main/java/com/ecommerce/projetobackend/
├── ProjetobackendApplication.java
├── auth/                  AuthController, AuthService, Register/Login Request, AuthResponse
├── cart/                  Cart, CartItem (mapeadas, sem service ativo)
├── config/                SecurityConfig, WebConfig
├── order/                 OrderController, Service, Repository, DTOs, Order, OrderItem, OrderStatus
├── product/               ProductController, Service, Repository, DTOs, Product, ProductStatus
├── security/              JwtService, JwtAuthFilter, JwtAuthenticationEntryPoint, UserDetailsImpl/ServiceImpl
├── shared/exception/      GlobalExceptionHandler, ErrorResponseDTO, exceptions de domínio
└── user/                  UserController, Service, Repository, User, UserRole, DTOs

src/main/resources/
├── application.yml        Profiles dev/prod, JWT, JPA, Flyway
└── db/migration/
    ├── V1__initial_schema.sql
    ├── V2__align_orders_to_mer.sql
    └── V3__seed_admin.sql
```

## Débitos técnicos conhecidos

Itens reconhecidos como fora de escopo desta entrega:

- **Cart dormindo** — tabelas `carts` e `cart_items` existem no schema mas nenhum service/controller as utiliza. Reservado pra evolução futura.
- **Sem testes automatizados** — apenas o smoke test default do Spring Boot (`contextLoads`). Validação foi feita manualmente via curl em cada fase.
- **Sem `@PreAuthorize`** — segurança por domínio fica nos services. Trade-off explicado em [Decisões de arquitetura](#decisões-de-arquitetura).
- **Sem refresh token** — JWT expira e o cliente precisa logar de novo. Fora de escopo.
- **Sem rate limiting** — qualquer cliente pode tentar login indefinidamente.

## Autores

- Tarso Monteiro Alves Passos
- Luka Santana Shrewsbury Rubino
- João Guilherme Costa Carvalho
- Raphael Paiva Almeida de Andrade Gomes
- Pedro Rodrigo do Carmo Coutinho
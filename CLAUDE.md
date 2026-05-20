# 5MKT - Marketplace Hub

API REST de marketplace acadêmica. Cadastro de usuários (CUSTOMER/SELLER/ADMIN), CRUD de produtos por vendedor, gestão de pedidos com controle de estoque transacional.

**Status do backend:** funcionalmente completo. README completo na raiz do repositório.

## Stack

- Java 21 (LTS), Spring Boot 4.0.5
- PostgreSQL 16, Flyway (migrations em `db/migration`)
- Spring Security + JWT (JJWT 0.12.6, HS384, sem refresh token)
- BCrypt strength 10
- Spring Data JPA + Hibernate
- Springdoc OpenAPI 3.0.x (Swagger UI em `/swagger-ui/index.html`)
- Maven, Lombok, Docker Compose
- Jackson 3 (`tools.jackson.*`, padrão do Spring Boot 4)

## Arquitetura

- API REST monolítica em camadas (Controller → Service → Repository).
- Organização de pacotes **por feature** (`com.ecommerce.projetobackend.{auth,user,product,order,cart,security,config,shared.exception}`).
- DTOs como **classes Lombok `@Data`** (não records). `@Builder` em responses quando útil.
- Conversão entity↔DTO via método estático `XxxResponse.from(XxxEntity)`.
- Erros via `@RestControllerAdvice` global, retornando `ErrorResponseDTO` consistente (timestamp, status, error, message, path, errors).
- Auth stateless via JWT. Roles: `ADMIN`, `SELLER`, `CUSTOMER`. Authorities prefixadas com `ROLE_`.
- `ddl-auto=validate`. Flyway controla schema (V1, V2, V3 aplicadas).
- **Autorização vive no service**, não com `@PreAuthorize`. Decisão consciente para legibilidade.

## Convenções

- **Entidades:** `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode(of="id")`. **NUNCA `@Data`** em entity (loop em toString com relações bidirecionais).
- **DTOs:** classes Lombok `@Data` com `@NoArgsConstructor @AllArgsConstructor`. `@Builder` quando necessário (responses).
- **Validações:** `jakarta.validation` (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@NotNull`, `@NotEmpty`, `@DecimalMin`, `@Valid`).
- **IDs:** `Long`, `@GeneratedValue(strategy = IDENTITY)`.
- **Datas:** `LocalDateTime` para timestamps de domínio, `Instant` em respostas de erro.
- **Money:** `BigDecimal` com `precision=12, scale=2`.
- **Tabelas:** snake_case plural (`users`, `products`, `orders`, `order_items`). Endpoints: `/api/v1/{recurso}`.
- **Mensagens e logs em inglês.** Comentários e documentação em PT-BR.
- **Timestamps:** `created_at` e `updated_at` em quase todas as tabelas, preenchidos por DEFAULT NOW() + trigger `set_updated_at`. Nas entities: `@Column(insertable=false, updatable=false)`. Após `save()`, fazer re-fetch via repository ou `entityManager.refresh()` para popular os valores.

## Regras de domínio

- **Pedido simples** (sem split por seller). 1 order pode ter itens de N vendedores. Order só tem `customer_id` (não tem `seller_id` — V2 removeu).
- **Cart e CartItem mapeadas como entities mas SEM service/controller.** Tabelas existem no banco, classes mapeiam, mas o fluxo de pedido não usa carrinho — cliente posta lista de itens direto em `POST /orders`. Reservado pra futuro.
- **Snapshot de preço:** `OrderItem.unitPrice` guarda o preço do produto no momento da compra. Mudanças de preço posteriores não afetam pedidos existentes.
- **Controle de estoque transacional:** `OrderService.create` e `OrderService.cancel` usam `@Transactional`. Demais services NÃO usam `@Transactional` (re-fetch via novo contexto funciona melhor pros timestamps).
- **Anti user enumeration:** `BadCredentialsException` e `UsernameNotFoundException` retornam resposta byte-a-byte idêntica (401 + "Invalid credentials").

## Autorização (resumo)

| Endpoint | Acesso |
|---|---|
| `POST /api/v1/auth/register` | Público (aceita só CUSTOMER/SELLER; ADMIN → 400) |
| `POST /api/v1/auth/login` | Público |
| `GET /api/v1/products`, `GET /api/v1/products/{id}` | Público |
| `POST /api/v1/products` | SELLER |
| `PUT/DELETE /api/v1/products/{id}` | Dono OU ADMIN |
| `POST /api/v1/orders` | CUSTOMER |
| `GET /api/v1/orders`, `GET /api/v1/orders/{id}`, cancel | CUSTOMER dono OU ADMIN. SELLER bloqueado. |
| `GET /api/v1/users/me`, `PUT /api/v1/users/me` | Autenticado |
| `GET /api/v1/users`, `GET /api/v1/users/{id}` | ADMIN |

## Admin seedado (V3)

- **Email:** `admin@mkt5.com`
- **Senha:** `admin12345` (BCrypt hash gerado localmente no V3, **não regenerar**)

## Fases concluídas

| Fase | Commit | Conteúdo |
|---|---|---|
| 0 | 6077b88 | Setup inicial |
| 1 | 0c8b1a9 | Docker + V1 schema + application.yml |
| 2 | eb46c8b | Entidades JPA |
| — | d955c57 | chore: remove target/ tracking |
| 3a | 03e5abf | Auth infra (JwtService, JwtAuthFilter, SecurityConfig) |
| — | 2d570e6 | chore: alinhar orders ao MER + remove amqp |
| 3b | ef8535f | Auth pública + ExceptionHandler global |
| 4 | 7ef907b | Product CRUD com autorização |
| 5 | 3519721 | Order (create/list/findById/cancel) + controle de estoque |
| 6 | b2d2393 | User CRUD mínimo + seed ADMIN |
| 7a | d9bb2c6 | ObjectMapper Jackson 3 + paginação VIA_DTO |
| 7b | (next) | README |
| 7c | (next) | Atualização deste CLAUDE.md |

## Débitos técnicos reconhecidos

- **Cart dormindo** — tabelas mapeadas, sem service/controller. Ativar criando service+controller, sem migration nova.
- **Sem testes automatizados** — só o smoke `contextLoads` do Spring Boot.
- **Sem refresh token** — JWT expira, cliente loga de novo.
- **Sem rate limiting** — sem proteção contra brute force no login.

## Como o Claude Code deve trabalhar

- **Antes de codar, apresentar plano e aguardar OK explícito.**
- **Não rodar `mvn clean install` automaticamente.** Usar `./mvnw clean compile` para validação durante desenvolvimento.
- **Não criar arquivos fora do escopo sem perguntar.**
- **Mudanças em pom/schema/segurança/contrato: SEMPRE perguntar antes.** Isso inclui: alterar dependências, criar migrations, mexer em SecurityConfig, adicionar handlers no GlobalExceptionHandler, mudar formato de DTOs já existentes, adicionar queries em repositories existentes.
- **Migrations são imutáveis após aplicadas.** Nunca editar V1, V2, V3 ou anteriores. Mudanças posteriores entram como V4+.
- **Não usar `@Transactional` por padrão.** Apenas em operações que exigem atomicidade explícita (OrderService.create/cancel). Em todos os outros services o re-fetch via novo contexto produz comportamento mais limpo.
- **Validar com `cat`** (não com `read_file` truncado) ao final de cada fase. Apresentar curls completos + git status + git diff --stat.
- **Não oferecer merge/push/discard ao final.** Apenas implementar, validar, e aguardar próxima instrução.
- **Documentar desvios ANTES de executar**, não no resumo final.
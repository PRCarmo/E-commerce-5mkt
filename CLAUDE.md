# 5MKT - Marketplace Hub

Projeto da disciplina de Programação Web-Backend, UNIT.
Grupo: 5 devs. Cada feature é desenvolvida em branch separada.

## Stack
- Java 21 (LTS), Spring Boot 4.0.5
- PostgreSQL 16, Flyway (migrations em db/migration)
- Spring Security + JWT (JJWT 0.12.x), BCrypt
- Spring Data JPA + Hibernate
- RabbitMQ via Spring AMQP (notificações assíncronas)
- Springdoc OpenAPI 3.0.x (Swagger UI em /swagger-ui.html)
- Maven, Lombok, Docker Compose

## Arquitetura
- API REST monolítica em camadas (Controller → Service → Repository).
- Organização de pacotes POR FEATURE, não por camada.
  com.ecommerce.projetobackend.{user,product,cart,order,notification,security,config,shared}
- DTOs são records imutáveis. Conversão entity↔DTO em método estático do record.
- Erros seguem RFC 7807 (ProblemDetail). @RestControllerAdvice em shared/exception.
- Auth stateless via JWT. Roles: ADMIN, SELLER, CUSTOMER.
- ddl-auto=validate sempre. Schema controlado por Flyway.

## Convenções de código
- Entidades JPA: @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  @EqualsAndHashCode(of = "id"). NUNCA @Data em entity (quebra lazy loading).
- DTOs: records + jakarta.validation (@NotBlank, @Positive, etc).
- IDs: Long, @GeneratedValue(strategy = IDENTITY).
- Datas: LocalDateTime (UTC), nunca Date.
- Money: BigDecimal, nunca double/float.
- Nomenclatura de tabelas: snake_case plural (users, products, order_items).
- Endpoints: /api/v1/{recurso}, plural.
- Mensagens de exceção e logs em inglês, mas comentários e docs em PT-BR.

## Regra de pedido (decisão de domínio)
- Carrinho persiste no banco (Cart + CartItem por usuário).
- Checkout cria UMA Order por seller distinto no carrinho. Cada Order tem:
  customer (User), seller (User), itens, status próprio, total próprio.

## Como evoluir
- Cada nova feature: branch `feat/fase-N-nome`. PR pequeno e focado.
- Migrations Flyway são imutáveis depois de mergeadas. Criar V2, V3...
- Nunca alterar V1 depois do primeiro merge.

## Como o Claude Code deve trabalhar aqui
- Antes de codar, propor plano (arquivos + decisões) e aguardar OK.
- Não rodar `mvn clean install` automaticamente. Apenas escrever código.
- Não criar arquivos fora do escopo do prompt sem perguntar.
- Se faltar info, perguntar antes de assumir.
# Fase 3a — Auth Infrastructure (Spring Security + JWT) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Instalar a infraestrutura stateless de Spring Security + JWT para que rotas não-públicas retornem 401 e o Swagger/auth endpoints permaneçam acessíveis — sem nenhum endpoint REST ainda.

**Architecture:** Filter chain stateless com `JwtAuthFilter extends OncePerRequestFilter` plugado antes do `UsernamePasswordAuthenticationFilter`. `UserDetailsServiceImpl` carrega o usuário a cada requisição pelo email do subject do token. `JwtService` encapsula toda lógica JJWT 0.12.x.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Spring Security 6, JJWT 0.12.6 (já no pom.xml), Lombok, Jakarta Validation.

---

## Mapa de arquivos

| Arquivo | Ação | Responsabilidade |
|---------|------|-----------------|
| `user/UserRepository.java` | Criar | Interface JPA para User |
| `user/UserService.java` | Criar | Fachada de domínio (read-only por ora) |
| `security/JwtService.java` | Criar | Geração e validação de tokens JJWT 0.12.x |
| `security/UserDetailsImpl.java` | Criar | Wrapper UserDetails em torno de User |
| `security/UserDetailsServiceImpl.java` | Criar | Carrega UserDetails pelo email |
| `security/JwtAuthFilter.java` | Criar | Lê header Authorization e popula SecurityContext |
| `config/SecurityConfig.java` | Criar | Configura filter chain, CORS, rotas públicas |

Nenhum arquivo existente precisa ser modificado nesta fase.

---

## Decisões técnicas fechadas

(Listadas aqui para referência; detalhes no enunciado.)

1. **JJWT API:** `Jwts.builder().claims().subject(...).add(...).expiration(...).signWith(key)` — API 0.12.x. Parsing via `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()`.
2. **Algoritmo:** HS256. Chave: `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`, inicializada em `@PostConstruct`.
3. **Claims:** `sub` = email, custom `userId` (Long), custom `role` (String).
4. **`UserDetailsImpl`:** wrapper; `User` NÃO implementa `UserDetails`.
5. **Authorities:** `"ROLE_" + user.getRole().name()` via `SimpleGrantedAuthority`.
6. **`JwtAuthFilter`:** em qualquer exceção (JwtException, UsernameNotFoundException, etc.) apenas chama `filterChain.doFilter` sem popular contexto; entry point cuida do 401.
7. **`SecurityConfig`:** injeta `JwtAuthFilter` por construtor (sem `@Autowired`).

## Decisões adicionais (não explicitadas no enunciado)

8. **CORS com `allowCredentials: true`:** Spring Security 6 proíbe `allowedOrigins("*")` combinado com `allowCredentials(true)`. Usar `allowedOriginPatterns("*")` — comportamento idêntico para o escopo de disciplina.
9. **`@Bean AuthenticationManager`:** o método `authenticationConfiguration.getAuthenticationManager()` lança checked `Exception`; o bean method declara `throws Exception` (padrão Spring).
10. **`application.properties`:** contém apenas `spring.application.name=projetobackend` (gerado pelo initializr). Não conflita com `application.yml` — mantém como está.
11. **`JwtService.extractEmail`:** propaga `JwtException` (unchecked) se o token for inválido/expirado. O filter captura e não propaga.

---

## Tarefa 1 — `UserRepository`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/user/UserRepository.java`

- [ ] Criar a interface `UserRepository extends JpaRepository<User, Long>` com os métodos:
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
- [ ] Compilar: `./mvnw clean compile` → **BUILD SUCCESS**
- [ ] Commit: `feat: add UserRepository`

---

## Tarefa 2 — `UserService`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/user/UserService.java`

- [ ] Criar `@Service UserService` com construtor injetando `UserRepository`.
- [ ] Métodos públicos:
  - `Optional<User> findByEmail(String email)`
  - `Optional<User> findById(Long id)`
  - `boolean existsByEmail(String email)`
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add UserService (read-only)`

---

## Tarefa 3 — `UserDetailsImpl`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/security/UserDetailsImpl.java`

- [ ] Criar classe implementando `UserDetails`.
- [ ] Campo `private final User user`.
- [ ] Construtor recebe `User`.
- [ ] `getUsername()` → `user.getEmail()`
- [ ] `getPassword()` → `user.getPassword()`
- [ ] `getAuthorities()` → `List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))`
- [ ] `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isEnabled()` → todos `true`
- [ ] `getUser()` → retorna `user` (acesso à entidade em controllers futuros)
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add UserDetailsImpl`

---

## Tarefa 4 — `UserDetailsServiceImpl`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/security/UserDetailsServiceImpl.java`

- [ ] Criar `@Service UserDetailsServiceImpl implements UserDetailsService`.
- [ ] Construtor injeta `UserService`.
- [ ] `loadUserByUsername(String email)`:
  - Busca via `UserService.findByEmail(email)`
  - Se não encontrar: lança `UsernameNotFoundException("User not found: " + email)`
  - Se encontrar: retorna `new UserDetailsImpl(user)`
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add UserDetailsServiceImpl`

---

## Tarefa 5 — `JwtService`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/security/JwtService.java`

- [ ] Criar `@Service JwtService`.
- [ ] `@Value("${app.jwt.secret}") String secret` e `@Value("${app.jwt.expiration-ms}") long expirationMs`.
- [ ] Campo `private SecretKey secretKey` inicializado em `@PostConstruct` via `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`.
- [ ] `String generateToken(User user)`:
  - Usa `Jwts.builder()` — API 0.12.x
  - `claims()` → `.subject(user.getEmail()).add("userId", user.getId()).add("role", user.getRole().name())`
  - `issuedAt(Date.from(Instant.now()))`
  - `expiration(Date.from(Instant.now().plusMillis(expirationMs)))`
  - `signWith(secretKey)`
  - `.compact()`
- [ ] `String extractEmail(String token)`:
  - `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject()`
  - Propaga `JwtException` se inválido (não capturar aqui)
- [ ] `boolean isTokenValid(String token, String email)`:
  - Chama `extractEmail(token)` e compara com o email recebido
  - Retorna `true` se iguais
  - Qualquer `JwtException` → retorna `false`
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add JwtService`

---

## Tarefa 6 — `JwtAuthFilter`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/security/JwtAuthFilter.java`

- [ ] Criar `@Component JwtAuthFilter extends OncePerRequestFilter`.
- [ ] Construtor injeta `JwtService` e `UserDetailsService`.
- [ ] `doFilterInternal`:
  1. Lê `Authorization` header; se ausente ou não começa com `"Bearer "` → `filterChain.doFilter` e return.
  2. Extrai token (substring após `"Bearer "`).
  3. Extrai email via `jwtService.extractEmail(token)` — em exceção: `filterChain.doFilter` e return.
  4. Se email não nulo e `SecurityContextHolder.getContext().getAuthentication() == null`:
     - Carrega `UserDetails` via `userDetailsService.loadUserByUsername(email)` — em exceção: `filterChain.doFilter` e return.
     - Valida token via `jwtService.isTokenValid(token, userDetails.getUsername())`.
     - Se válido: cria `UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())`, seta `WebAuthenticationDetailsSource` details, popula `SecurityContextHolder`.
  5. `filterChain.doFilter(request, response)`.
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add JwtAuthFilter`

---

## Tarefa 7 — `SecurityConfig`

**Arquivo:** `src/main/java/com/ecommerce/projetobackend/config/SecurityConfig.java`

- [ ] Criar `@Configuration @EnableWebSecurity @EnableMethodSecurity SecurityConfig`.
- [ ] Construtor injeta `JwtAuthFilter`.
- [ ] `@Bean SecurityFilterChain`:
  - `csrf(AbstractHttpConfigurer::disable)`
  - `cors(Customizer.withDefaults())`
  - `sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`
  - `authorizeHttpRequests`:
    - `.requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()`
    - `.anyRequest().authenticated()`
  - `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`
- [ ] `@Bean PasswordEncoder` → `new BCryptPasswordEncoder()` (strength padrão 10)
- [ ] `@Bean AuthenticationManager(AuthenticationConfiguration config) throws Exception` → `config.getAuthenticationManager()`
- [ ] `@Bean CorsConfigurationSource`:
  - `cfg.setAllowedOriginPatterns(List.of("*"))` ← **`allowedOriginPatterns`, não `allowedOrigins`** (Spring Security 6 + `allowCredentials=true`)
  - `cfg.setAllowedMethods(List.of("*"))`
  - `cfg.setAllowedHeaders(List.of("*"))`
  - `cfg.setAllowCredentials(true)`
  - `UrlBasedCorsConfigurationSource` registrando `"/**"`
- [ ] Compilar → **BUILD SUCCESS**
- [ ] Commit: `feat: add SecurityConfig`

---

## Tarefa 8 — Validação de runtime

> Requer banco PostgreSQL rodando (`docker compose up -d`) pois `ddl-auto=validate`.

- [ ] `./mvnw spring-boot:run` — sobe sem erro.
- [ ] Verificar nos logs: **ausência** de `"Using generated security password"` (confirma que nosso `UserDetailsService` está ativo).
- [ ] Em outro terminal:
  ```
  curl -i http://localhost:8080/swagger-ui.html         # espera 302 ou 200
  curl -i http://localhost:8080/v3/api-docs             # espera 200
  curl -i http://localhost:8080/api/v1/qualquer-coisa   # espera 401
  curl -i -H "Authorization: Bearer tokeninvalido" http://localhost:8080/api/v1/qualquer-coisa  # espera 401
  ```
- [ ] Ctrl+C para parar.
- [ ] Commit final se não houve desde a última tarefa.

---

## Checklist de cobertura do escopo

| Requisito | Tarefa |
|-----------|--------|
| `UserRepository` com `findByEmail` e `existsByEmail` | Tarefa 1 |
| `UserService` read-only | Tarefa 2 |
| `UserDetailsImpl` wrapper (sem misturar com entidade) | Tarefa 3 |
| `UserDetailsServiceImpl` com `UsernameNotFoundException` | Tarefa 4 |
| `JwtService` com API JJWT 0.12.x, HS256, claims corretos | Tarefa 5 |
| `JwtAuthFilter` com tratamento de exceção silent | Tarefa 6 |
| `SecurityConfig` stateless, CORS, rotas públicas, `@EnableMethodSecurity` | Tarefa 7 |
| Validação de runtime com 4 curls | Tarefa 8 |
| Sem endpoints REST / DTOs / exception handler global | nenhum criado |

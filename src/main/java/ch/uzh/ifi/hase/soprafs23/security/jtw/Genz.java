@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebSecurityConfigurer<WebSecurity> {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Ignore Swagger UI resources
        web.ignoring()
                .antMatchers("/swagger-ui.html")
                .antMatchers("/swagger-ui/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request.getHeader(AUTHORIZATION_HEADER));
        if (token != null) {
            try {
                String username = validateToken(token);
                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // Handle invalid token
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(String header) {
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private String validateToken(String token) {
        return Jwts.parserBuilder().setSigningKey("<YOUR-SECRET-KEY>").build().parseClaimsJws(token).getBody().getSubject();
    }
}
``

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            String token = jwtTokenProvider.createToken(loginRequest.getUsername());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            String username = jwtTokenProvider.validateRefreshToken(refreshTokenRequest.getRefreshToken());
            String token = jwtTokenProvider.createToken(username);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

@Service
public class JwtTokenProvider {

    private static final long TOKEN_EXPIRATION_TIME = 60 * 60 * 1000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 30 * 24 * 60 * 60 * 1000; // 30 days

    private final String secretKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String createToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRATION_TIME);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_TIME);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    public String validateRefreshToken(String refreshToken) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(refreshToken).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                new ArrayList<>());
    }
}

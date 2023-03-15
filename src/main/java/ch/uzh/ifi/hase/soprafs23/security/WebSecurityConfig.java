@Bean
public AuthenticationManager authenticationManagerBean() throws Exception {
        return authenticationManager();
        }

@Bean
public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService());
        return provider;
        }

@Bean
public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        }

@Bean
public UserDetailsService userDetailsService() {
        return new MyUserDetailsService();
        }

@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
        .authorizeExchange()
        .pathMatchers("/public/**").permitAll()
        .anyExchange().authenticated()
        .and()
        .httpBasic()
        .and()
        .csrf().disable()
        .build();
        }
package ch.uzh.ifi.hase.soprafs23.security.xnxx;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public FilterRegistrationBean<JwtAuthorizationFilter> jwtAuthorizationFilterRegistrationBean(
            AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserDetailsService userDetailsService) {
        FilterRegistrationBean<JwtAuthorizationFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(authenticationManager, jwtUtil,
                userDetailsService);
        filterRegistrationBean.setFilter(jwtAuthorizationFilter);
        filterRegistrationBean.addUrlPatterns("/secured/*");
        return filterRegistrationBean;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public JWTUtil jwtUtil() {
        return new JWTUtil();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // implement user details loading logic
            }
        };
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }


}


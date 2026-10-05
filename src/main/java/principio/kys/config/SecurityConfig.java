package principio.kys.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import principio.kys.service.PresenciaService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PresenciaService presenciaService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // [P0] CSRF activo para toda la app (los forms Thymeleaf con th:action incluyen el token
        // automáticamente). /api/** queda exento porque los clientes JSON no envían token.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // token siempre disponible en ${_csrf}

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
                .csrfTokenRequestHandler(csrfHandler)
            )
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/registro", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // API publica minima: login JSON/body (evita exponer resto sin auth)
                .requestMatchers("/api/usuarios/login").permitAll()
                .requestMatchers("/admin/**").hasRole("DIRECTOR")
                .requestMatchers("/profesor/**").hasAnyRole("DIRECTOR", "PROFESOR")
                .requestMatchers("/alumno/**").hasAnyRole("DIRECTOR", "ALUMNO")
                .requestMatchers("/api/usuarios/crear-director").hasRole("DIRECTOR")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            // [P0] Acceso denegado => 403 estándar. Para navegadores, Spring Boot renderiza
            // templates/error.html vía /error (con homeUrl según rol); para API, JSON de error.
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    presenciaService.conectar(authentication.getName());
                    String rol = authentication.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .findFirst().orElse("");
                    if (rol.equals("ROLE_DIRECTOR")) {
                        response.sendRedirect("/admin");
                    } else if (rol.equals("ROLE_PROFESOR")) {
                        response.sendRedirect("/profesor");
                    } else {
                        response.sendRedirect("/alumno");
                    }
                })
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                // [P0] logout por POST (el form de la sidebar envía el token CSRF)
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        presenciaService.desconectar(authentication.getName());
                    }
                    response.sendRedirect("/login");
                })
                .permitAll()
            );
        return http.build();
    }
}

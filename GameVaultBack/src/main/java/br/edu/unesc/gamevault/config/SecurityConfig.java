package br.edu.unesc.gamevault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.edu.unesc.gamevault.security.AcessoNegadoManipulador;
import br.edu.unesc.gamevault.security.EntradaNaoAutenticada;
import br.edu.unesc.gamevault.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] ROTAS_SWAGGER = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final EntradaNaoAutenticada entradaNaoAutenticada;
    private final AcessoNegadoManipulador acessoNegadoManipulador;

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(tratamento -> tratamento
                        .authenticationEntryPoint(entradaNaoAutenticada)
                        .accessDeniedHandler(acessoNegadoManipulador))
                .authorizeHttpRequests(requisicoes -> requisicoes
                        // Autenticação e documentação
                        .requestMatchers(HttpMethod.POST, "/auth/registrar", "/auth/login").permitAll()
                        .requestMatchers(ROTAS_SWAGGER).permitAll()

                        // Precisa vir antes do GET /jogos/*, senão /jogos/meus cairia na regra pública
                        .requestMatchers(HttpMethod.GET, "/jogos/meus", "/jogos/meus/vendas").authenticated()

                        // Catálogo é público: vitrine da loja
                        .requestMatchers(HttpMethod.GET, "/jogos", "/jogos/*", "/jogos/*/avaliacoes",
                                "/categorias", "/categorias/*")
                        .permitAll()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager gerenciadorDeAutenticacao(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }
}

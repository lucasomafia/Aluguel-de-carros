package br.com.aluguel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain cadeiaFiltrosSeguranca(HttpSecurity segurancaHttp) throws Exception {
        return segurancaHttp
            .csrf(protecaoCsrf -> protecaoCsrf.disable())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cacheRequisicoes -> cacheRequisicoes.disable())
            .authorizeHttpRequests(autorizacao -> autorizacao
                .requestMatchers(HttpMethod.POST, "/api/auth/cadastro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/automoveis", "/api/automoveis/*").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .formLogin(formulario -> formulario.disable())
            .logout(saida -> saida.disable())
            .build();
    }
}

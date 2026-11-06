package br.com.aluguel.config;

import br.com.aluguel.model.Perfil;
import br.com.aluguel.repository.UsuarioRepository;
import br.com.aluguel.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class AgenteInicialConfig {
    @Bean
    CommandLineRunner criarAgenteInicial(
        UsuarioRepository repositorio, UsuarioService servico,
        @Value("${app.agente.usuario:}") String nomeUsuario,
        @Value("${app.agente.senha:}") String senha,
        @Value("${app.agente.nome:Agente inicial}") String nome,
        @Value("${app.agente.perfil:EMPRESA}") Perfil perfil
    ) {
        return argumentos -> {
            if (nomeUsuario.isBlank() && senha.isBlank()) return;
            if (!nomeUsuario.matches("[a-zA-Z0-9._-]{3,60}") || senha.length() < 8 || senha.length() > 64
                || senha.getBytes(StandardCharsets.UTF_8).length > 72 || nome.isBlank() || nome.length() > 120
                || perfil == Perfil.CLIENTE) {
                throw new IllegalArgumentException("Configuração do agente inválida. Confira AGENTE_USUARIO, AGENTE_SENHA, AGENTE_NOME e AGENTE_PERFIL.");
            }
            if (!repositorio.existsByNomeUsuario(nomeUsuario.toLowerCase(Locale.ROOT))) {
                servico.criar(nome.strip(), nomeUsuario, senha, perfil);
            }
        };
    }
}

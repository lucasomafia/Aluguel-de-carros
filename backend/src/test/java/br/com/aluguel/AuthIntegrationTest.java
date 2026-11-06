package br.com.aluguel;

import br.com.aluguel.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
    @Value("${local.server.port}") int porta;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificadorSenha;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    @Test
    void cadastraClienteSemExporSenhaEArmazenaHash() throws Exception {
        String nomeUsuario = novoNomeUsuario();
        var resposta = cadastrar(nomeUsuario, "Senha@123", "");
        assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(resposta.body()).contains("CLIENTE", nomeUsuario).doesNotContain("senha", "Senha@123", "senhaHash");
        var usuario = usuarios.findByNomeUsuario(nomeUsuario).orElseThrow();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("Senha@123");
        assertThat(codificadorSenha.matches("Senha@123", usuario.getSenhaHash())).isTrue();
    }

    @Test
    void autenticaEConsultaUsuario() throws Exception {
        String nomeUsuario = novoNomeUsuario();
        cadastrar(nomeUsuario, "Senha@123", "");
        var login = enviar("POST", "/api/auth/login", null, nomeUsuario, "Senha@123");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains(nomeUsuario).doesNotContain("senha");
        var dadosUsuario = enviar("GET", "/api/auth/me", null, nomeUsuario, "Senha@123");
        assertThat(dadosUsuario.statusCode()).isEqualTo(200);
        assertThat(dadosUsuario.body()).contains(nomeUsuario);
    }

    @Test
    void rejeitaAcessoSemLoginOuComSenhaErrada() throws Exception {
        assertThat(enviar("GET", "/api/auth/me", null, null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/auth/login", null, "inexistente", "Senha@123").statusCode()).isEqualTo(401);
        String nomeUsuario = novoNomeUsuario();
        cadastrar(nomeUsuario, "Senha@123", "");
        assertThat(enviar("POST", "/api/auth/login", null, nomeUsuario, "errada").statusCode()).isEqualTo(401);
    }

    @Test
    void rejeitaUsuarioDuplicadoIgnorandoMaiusculas() throws Exception {
        String nomeUsuario = novoNomeUsuario();
        cadastrar(nomeUsuario, "Senha@123", "");
        assertThat(cadastrar(nomeUsuario.toUpperCase(java.util.Locale.ROOT), "Senha@123", "").statusCode()).isEqualTo(409);
    }

    @Test
    void validaCamposESenhaEmBytes() throws Exception {
        assertThat(cadastrar("ab", "curta", "").statusCode()).isEqualTo(400);
        assertThat(cadastrar(novoNomeUsuario(), "é".repeat(40), "").statusCode()).isEqualTo(400);
        var resposta = enviar("POST", "/api/auth/cadastro", "{}", null, null);
        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("campos");
    }

    @Test
    void naoPermiteEscolherPerfilPrivilegiadoNoCadastroPublico() throws Exception {
        var resposta = cadastrar(novoNomeUsuario(), "Senha@123", ", \"perfil\": \"BANCO\"");
        assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(resposta.body()).contains("CLIENTE").doesNotContain("BANCO");
    }

    @Test
    void permiteLoginDoAgenteProvisionado() throws Exception {
        var resposta = enviar("POST", "/api/auth/login", null, "empresa", "Empresa@123");
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("EMPRESA").doesNotContain("Empresa@123");
    }

    @Test
    void cadastroExigeJson() throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/auth/cadastro"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Teste&nomeUsuario=teste&senha=Senha123")).build();
        assertThat(clienteHttp.send(requisicao, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(415);
    }

    private String novoNomeUsuario() {
        return "u" + UUID.randomUUID().toString().replace("-", "");
    }

    private HttpResponse<String> cadastrar(String nomeUsuario, String senha, String camposAdicionais) throws Exception {
        String json = "{\"nome\":\"Cliente Teste\",\"nomeUsuario\":\"%s\",\"senha\":\"%s\"%s}"
            .formatted(nomeUsuario, senha, camposAdicionais);
        return enviar("POST", "/api/auth/cadastro", json, null, null);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String nomeUsuario, String senha) throws Exception {
        var construtor = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/json");
        if (nomeUsuario != null) {
            String credencial = Base64.getEncoder().encodeToString((nomeUsuario + ":" + senha).getBytes(StandardCharsets.UTF_8));
            construtor.header("Authorization", "Basic " + credencial);
        }
        construtor.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        return clienteHttp.send(construtor.build(), HttpResponse.BodyHandlers.ofString());
    }
}

package br.com.aluguel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutomovelIntegrationTest {
    @Value("${local.server.port}") int porta;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final ObjectMapper conversor = new ObjectMapper();

    @Test
    void somenteUsuarioAutenticadoConsultaAutomoveisEAgentePodeGerenciar() throws Exception {
        String nomeCliente = "a" + UUID.randomUUID().toString().replace("-", "");
        enviar("POST", "/api/auth/cadastro", "{\"nome\":\"Cliente\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeCliente), null);
        String matricula = "MAT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String placa = "ABC1D23";
        String dados = "{\"matricula\":\"%s\",\"ano\":2024,\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"placa\":\"%s\"}".formatted(matricula, placa);
        assertThat(enviar("GET", "/api/automoveis", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/automoveis", null, nomeCliente).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/automoveis", dados, nomeCliente).statusCode()).isEqualTo(403);
        var criado = enviar("POST", "/api/automoveis", dados, "empresa");
        assertThat(criado.statusCode()).isEqualTo(201);
        JsonNode json = conversor.readTree(criado.body());
        String id = json.get("id").asText();
        assertThat(criado.body()).doesNotContain("proprietarioId");
        assertThat(enviar("GET", "/api/automoveis", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/automoveis", null, "empresa").body()).contains(id, "Fiat");
        assertThat(enviar("GET", "/api/automoveis", null, nomeCliente).body()).contains(id, "Fiat");
        assertThat(enviar("GET", "/api/automoveis/" + id, null, nomeCliente).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/automoveis", dados, "empresa").statusCode()).isEqualTo(409);
        assertThat(enviar("PUT", "/api/automoveis/" + id, dados.replace("Argo", "Cronos"), nomeCliente).statusCode()).isEqualTo(403);
        assertThat(enviar("PUT", "/api/automoveis/" + id, dados.replace("Argo", "Cronos"), "empresa").body()).contains("Cronos");
        assertThat(enviar("DELETE", "/api/automoveis/" + id, null, nomeCliente).statusCode()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/automoveis/" + id, null, "empresa").statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/api/automoveis/" + id, null, nomeCliente).statusCode()).isEqualTo(404);
    }

    @Test
    void rejeitaDadosInvalidos() throws Exception {
        assertThat(enviar("POST", "/api/automoveis", "{\"matricula\":\"X\",\"ano\":1800,\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"placa\":\"INVALIDA\"}", "empresa").statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String nomeUsuario) throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).header("Content-Type", "application/json");
        if (nomeUsuario != null) {
            String senha = nomeUsuario.equals("empresa") ? "Empresa@123" : "Senha@123";
            requisicao.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((nomeUsuario + ":" + senha).getBytes(StandardCharsets.UTF_8)));
        }
        requisicao.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        return clienteHttp.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }
}

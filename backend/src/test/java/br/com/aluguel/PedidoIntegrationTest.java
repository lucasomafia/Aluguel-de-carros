package br.com.aluguel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.repository.UsuarioRepository;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidoIntegrationTest {
    @Value("${local.server.port}") int porta;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final ObjectMapper conversor = new ObjectMapper();
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;

    @Test
    void clienteCadastradoCriaPedidoEConsultaApenasOsSeus() throws Exception {
        String primeiro = "p" + UUID.randomUUID().toString().replace("-", "");
        String segundo = "p" + UUID.randomUUID().toString().replace("-", "");
        cadastrarUsuario(primeiro);
        cadastrarUsuario(segundo);
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String automovel = "{\"matricula\":\"MAT%s\",\"ano\":2024,\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"placa\":\"DEF4G56\"}"
            .formatted(sufixo);
        String automovelId = conversor.readTree(enviar("POST", "/api/automoveis", automovel, "empresa").body()).get("id").asText();
        LocalDate retirada = LocalDate.now().plusDays(2);
        String dados = "{\"automovelId\":\"%s\",\"dataRetirada\":\"%s\",\"dataDevolucao\":\"%s\"}"
            .formatted(automovelId, retirada, retirada.plusDays(3));

        assertThat(enviar("GET", "/api/pedidos/me", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/pedidos", dados, "empresa").statusCode()).isEqualTo(403);
        assertThat(enviar("POST", "/api/pedidos", dados, primeiro).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/api/pedidos/me", null, segundo).body()).isEqualTo("[]");

        String cpf = "9" + String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
        String cliente = "{\"rg\":\"12345\",\"cpf\":\"%s\",\"endereco\":\"Rua A\",\"profissao\":\"Professor\",\"vinculos\":[]}"
            .formatted(cpf);
        assertThat(enviar("PUT", "/api/clientes/me", cliente, primeiro).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/pedidos", dados.replace(retirada.toString(), retirada.minusDays(3).toString()), primeiro).statusCode()).isEqualTo(400);

        var criado = enviar("POST", "/api/pedidos", dados, primeiro);
        assertThat(criado.statusCode()).isEqualTo(201);
        JsonNode pedido = conversor.readTree(criado.body());
        assertThat(pedido.get("status").asText()).isEqualTo("PENDENTE_ANALISE");
        assertThat(pedido.get("automovel").get("id").asText()).isEqualTo(automovelId);
        assertThat(enviar("GET", "/api/pedidos/me", null, primeiro).body()).contains(pedido.get("id").asText());
        assertThat(enviar("GET", "/api/pedidos/me", null, segundo).body()).isEqualTo("[]");

        String id = pedido.get("id").asText();
        String alterado = dados.replace(retirada.plusDays(3).toString(), retirada.plusDays(5).toString());
        assertThat(enviar("PUT", "/api/pedidos/me/" + id, alterado, segundo).statusCode()).isEqualTo(404);
        assertThat(enviar("PATCH", "/api/pedidos/me/" + id + "/cancelar", null, segundo).statusCode()).isEqualTo(404);
        assertThat(enviar("GET", "/api/pedidos", null, primeiro).statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/api/pedidos", null, "empresa").body()).contains(id, cpf, "Professor");
        assertThat(enviar("PUT", "/api/pedidos/" + id, alterado, "empresa").body()).contains(retirada.plusDays(5).toString());
        assertThat(enviar("PUT", "/api/pedidos/me/" + id, dados, primeiro).body()).contains(retirada.plusDays(3).toString());

        String banco = "b" + UUID.randomUUID().toString().replace("-", "");
        usuarios.saveAndFlush(new Usuario("Banco", banco, codificador.encode("Senha@123"), Perfil.BANCO));
        assertThat(enviar("GET", "/api/pedidos", null, banco).body()).contains(id);
        assertThat(enviar("POST", "/api/pedidos/" + id + "/avaliacao",
            "{\"aprovado\":true,\"parecer\":\"Renda aprovada\"}", banco).body())
            .contains("APROVADO_AGUARDANDO_CONTRATO", "Renda aprovada");
        assertThat(enviar("GET", "/api/pedidos/me", null, primeiro).body()).contains("Renda aprovada");
        assertThat(enviar("PUT", "/api/pedidos/me/" + id, alterado, primeiro).statusCode()).isEqualTo(409);
        assertThat(enviar("PATCH", "/api/pedidos/me/" + id + "/cancelar", null, primeiro).statusCode()).isEqualTo(409);
        assertThat(enviar("POST", "/api/pedidos/" + id + "/avaliacao",
            "{\"aprovado\":false,\"parecer\":\"Novo parecer\"}", "empresa").statusCode()).isEqualTo(409);
    }

    @Test
    void clienteCancelaPedidoPendente() throws Exception {
        String nomeUsuario = "p" + UUID.randomUUID().toString().replace("-", "");
        cadastrarUsuario(nomeUsuario);
        String cpf = "8" + String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
        enviar("PUT", "/api/clientes/me", "{\"rg\":\"123\",\"cpf\":\"%s\",\"endereco\":\"Rua B\",\"profissao\":\"Analista\",\"vinculos\":[]}".formatted(cpf), nomeUsuario);
        String matricula = "MAT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String automovel = "{\"matricula\":\"%s\",\"ano\":2025,\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"placa\":\"GHI7J89\"}"
            .formatted(matricula);
        String automovelId = conversor.readTree(enviar("POST", "/api/automoveis", automovel, "empresa").body()).get("id").asText();
        LocalDate retirada = LocalDate.now().plusDays(4);
        String dados = "{\"automovelId\":\"%s\",\"dataRetirada\":\"%s\",\"dataDevolucao\":\"%s\"}"
            .formatted(automovelId, retirada, retirada.plusDays(2));
        String id = conversor.readTree(enviar("POST", "/api/pedidos", dados, nomeUsuario).body()).get("id").asText();
        assertThat(enviar("PATCH", "/api/pedidos/me/" + id + "/cancelar", null, nomeUsuario).body()).contains("CANCELADO");
        assertThat(enviar("PUT", "/api/pedidos/" + id, dados, "empresa").statusCode()).isEqualTo(409);
    }

    private void cadastrarUsuario(String nomeUsuario) throws Exception {
        assertThat(enviar("POST", "/api/auth/cadastro",
            "{\"nome\":\"Cliente\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeUsuario), null).statusCode()).isEqualTo(201);
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

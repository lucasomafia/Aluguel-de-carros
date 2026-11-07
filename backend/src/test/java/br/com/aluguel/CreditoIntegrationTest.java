package br.com.aluguel;

import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreditoIntegrationTest {
    @Value("${local.server.port}") int porta;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final ObjectMapper conversor = new ObjectMapper();

    @Test
    void bancoConcedeCreditoUnicoEClienteConsultaOSeu() throws Exception {
        String cliente = "c" + UUID.randomUUID().toString().replace("-", "");
        String outroCliente = "c" + UUID.randomUUID().toString().replace("-", "");
        cadastrarCliente(cliente);
        cadastrarCliente(outroCliente);
        String cpf = "6" + String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
        assertThat(enviar("PUT", "/api/clientes/me", "{\"rg\":\"123\",\"cpf\":\"%s\",\"endereco\":\"Rua A\",\"profissao\":\"Professor\",\"vinculos\":[]}".formatted(cpf), cliente).statusCode()).isEqualTo(200);

        String matricula = "MAT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String automovelId = conversor.readTree(enviar("POST", "/api/automoveis",
            "{\"matricula\":\"%s\",\"ano\":2025,\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"placa\":\"PQR7S89\"}".formatted(matricula), "empresa").body()).get("id").asText();
        LocalDate retirada = LocalDate.now().plusDays(30);
        String pedidoId = conversor.readTree(enviar("POST", "/api/pedidos",
            "{\"automovelId\":\"%s\",\"dataRetirada\":\"%s\",\"dataDevolucao\":\"%s\"}"
                .formatted(automovelId, retirada, retirada.plusDays(2)), cliente).body()).get("id").asText();
        assertThat(enviar("POST", "/api/pedidos/" + pedidoId + "/avaliacao",
            "{\"aprovado\":true,\"parecer\":\"Aprovado\"}", "empresa").statusCode()).isEqualTo(200);
        String contratoId = conversor.readTree(enviar("POST", "/api/contratos",
            "{\"pedidoId\":\"%s\",\"tipoPropriedade\":\"EMPRESA\"}".formatted(pedidoId), cliente).body()).get("id").asText();

        String banco = "b" + UUID.randomUUID().toString().replace("-", "");
        usuarios.saveAndFlush(new Usuario("Banco", banco, codificador.encode("Senha@123"), Perfil.BANCO));
        String dados = "{\"contratoId\":\"%s\",\"valor\":15000.00,\"parcelas\":12}".formatted(contratoId);
        assertThat(enviar("GET", "/api/creditos/me", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/creditos", null, cliente).statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/api/creditos", null, "empresa").statusCode()).isEqualTo(403);
        assertThat(enviar("POST", "/api/creditos", dados, cliente).statusCode()).isEqualTo(403);
        assertThat(enviar("POST", "/api/creditos", dados, "empresa").statusCode()).isEqualTo(403);
        assertThat(enviar("POST", "/api/creditos", dados.replace("15000.00", "0"), banco).statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/creditos", dados.replace("\"parcelas\":12", "\"parcelas\":121"), banco).statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/creditos", dados.replace(contratoId, UUID.randomUUID().toString()), banco).statusCode()).isEqualTo(404);

        var criado = enviar("POST", "/api/creditos", dados, banco);
        assertThat(criado.statusCode()).isEqualTo(201);
        assertThat(criado.body()).contains(contratoId, "15000", "12", "Banco");
        assertThat(enviar("POST", "/api/creditos", dados, banco).statusCode()).isEqualTo(409);
        assertThat(enviar("GET", "/api/creditos/me", null, cliente).body()).contains(contratoId);
        assertThat(enviar("GET", "/api/creditos/me", null, outroCliente).body()).isEqualTo("[]");
        assertThat(enviar("GET", "/api/creditos", null, banco).body()).contains(contratoId);
    }

    private void cadastrarCliente(String nomeUsuario) throws Exception {
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

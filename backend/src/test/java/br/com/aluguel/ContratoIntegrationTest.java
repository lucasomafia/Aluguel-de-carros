package br.com.aluguel;

import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.repository.AutomovelRepository;
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
class ContratoIntegrationTest {
    @Value("${local.server.port}") int porta;
    @Autowired UsuarioRepository usuarios;
    @Autowired AutomovelRepository automoveis;
    @Autowired PasswordEncoder codificador;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final ObjectMapper conversor = new ObjectMapper();

    @Test
    void clienteExecutaContratoAprovadoUmaVezERegistraProprietario() throws Exception {
        String primeiro = cadastrarCliente();
        String segundo = cadastrarCliente();
        String automovelId = cadastrarAutomovel("JKL1M23");
        LocalDate retirada = LocalDate.now().plusDays(3);
        String pedidoId = criarPedido(primeiro, automovelId, retirada, retirada.plusDays(3));
        String dados = "{\"pedidoId\":\"%s\",\"tipoPropriedade\":\"CLIENTE\"}".formatted(pedidoId);

        assertThat(enviar("GET", "/api/contratos/me", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/contratos", dados, primeiro).statusCode()).isEqualTo(409);
        assertThat(enviar("POST", "/api/contratos", dados, segundo).statusCode()).isEqualTo(404);
        assertThat(enviar("POST", "/api/contratos", dados, "empresa").statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/api/contratos", null, primeiro).statusCode()).isEqualTo(403);

        avaliar(pedidoId, "empresa", true);
        assertThat(enviar("POST", "/api/contratos", dados.replace("CLIENTE", "BANCO"), primeiro).statusCode()).isEqualTo(400);
        var criado = enviar("POST", "/api/contratos", dados, primeiro);
        assertThat(criado.statusCode()).isEqualTo(201);
        assertThat(criado.body()).contains("CLIENTE", pedidoId, automovelId);
        assertThat(enviar("POST", "/api/contratos", dados, primeiro).statusCode()).isEqualTo(409);
        assertThat(enviar("GET", "/api/pedidos/me", null, primeiro).body()).contains("CONTRATADO");
        assertThat(enviar("GET", "/api/contratos/me", null, primeiro).body()).contains(pedidoId);
        assertThat(enviar("GET", "/api/contratos/me", null, segundo).body()).isEqualTo("[]");
        assertThat(enviar("GET", "/api/contratos", null, "empresa").body()).contains(pedidoId);
        assertThat(automoveis.findById(UUID.fromString(automovelId)).orElseThrow().getProprietario().getId())
            .isEqualTo(usuarios.findByNomeUsuario(primeiro).orElseThrow().getId());

        String sobreposto = criarPedido(segundo, automovelId, retirada.plusDays(1), retirada.plusDays(4));
        avaliar(sobreposto, "empresa", true);
        assertThat(enviar("POST", "/api/contratos", "{\"pedidoId\":\"%s\",\"tipoPropriedade\":\"EMPRESA\"}".formatted(sobreposto), segundo).statusCode()).isEqualTo(409);
    }

    @Test
    void contratoPodeRegistrarBancoComoProprietario() throws Exception {
        String cliente = cadastrarCliente();
        String banco = "b" + UUID.randomUUID().toString().replace("-", "");
        usuarios.saveAndFlush(new Usuario("Banco", banco, codificador.encode("Senha@123"), Perfil.BANCO));
        String automovelId = cadastrarAutomovel("MNO4P56");
        LocalDate retirada = LocalDate.now().plusDays(10);
        String pedidoId = criarPedido(cliente, automovelId, retirada, retirada.plusDays(2));
        avaliar(pedidoId, banco, true);
        var criado = enviar("POST", "/api/contratos",
            "{\"pedidoId\":\"%s\",\"tipoPropriedade\":\"BANCO\"}".formatted(pedidoId), cliente);
        assertThat(criado.statusCode()).isEqualTo(201);
        assertThat(criado.body()).contains("BANCO", "Banco");
        assertThat(automoveis.findById(UUID.fromString(automovelId)).orElseThrow().getProprietario().getId())
            .isEqualTo(usuarios.findByNomeUsuario(banco).orElseThrow().getId());
    }

    private String cadastrarCliente() throws Exception {
        String nomeUsuario = "c" + UUID.randomUUID().toString().replace("-", "");
        enviar("POST", "/api/auth/cadastro", "{\"nome\":\"Cliente\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeUsuario), null);
        String cpf = "7" + String.format("%010d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
        assertThat(enviar("PUT", "/api/clientes/me", "{\"rg\":\"123\",\"cpf\":\"%s\",\"endereco\":\"Rua A\",\"profissao\":\"Professor\",\"vinculos\":[]}".formatted(cpf), nomeUsuario).statusCode()).isEqualTo(200);
        return nomeUsuario;
    }

    private String cadastrarAutomovel(String placa) throws Exception {
        String matricula = "MAT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var resposta = enviar("POST", "/api/automoveis",
            "{\"matricula\":\"%s\",\"ano\":2025,\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"placa\":\"%s\"}".formatted(matricula, placa), "empresa");
        assertThat(resposta.statusCode()).isEqualTo(201);
        return conversor.readTree(resposta.body()).get("id").asText();
    }

    private String criarPedido(String cliente, String automovelId, LocalDate retirada, LocalDate devolucao) throws Exception {
        var resposta = enviar("POST", "/api/pedidos",
            "{\"automovelId\":\"%s\",\"dataRetirada\":\"%s\",\"dataDevolucao\":\"%s\"}"
                .formatted(automovelId, retirada, devolucao), cliente);
        assertThat(resposta.statusCode()).isEqualTo(201);
        return conversor.readTree(resposta.body()).get("id").asText();
    }

    private void avaliar(String pedidoId, String agente, boolean aprovado) throws Exception {
        assertThat(enviar("POST", "/api/pedidos/" + pedidoId + "/avaliacao",
            "{\"aprovado\":%s,\"parecer\":\"Aprovado\"}".formatted(aprovado), agente).statusCode()).isEqualTo(200);
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

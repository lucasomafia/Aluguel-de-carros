package br.com.aluguel;

import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.model.Pedido;
import br.com.aluguel.repository.AutomovelRepository;
import br.com.aluguel.repository.PedidoRepository;
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
    @Autowired PedidoRepository pedidos;
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
        String pendenteSobreposto = criarPedido(segundo, automovelId, retirada.plusDays(1), retirada.plusDays(4));
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

        String sobreposto = "{\"automovelId\":\"%s\",\"dataRetirada\":\"%s\",\"dataDevolucao\":\"%s\"}"
            .formatted(automovelId, retirada.plusDays(1), retirada.plusDays(4));
        var recusado = enviar("POST", "/api/pedidos", sobreposto, segundo);
        assertThat(recusado.statusCode()).isEqualTo(409);
        assertThat(recusado.body()).contains("já possui contrato");
        assertThat(enviar("PUT", "/api/pedidos/me/" + pendenteSobreposto, sobreposto, segundo).statusCode()).isEqualTo(409);
        assertThat(enviar("POST", "/api/pedidos/" + pendenteSobreposto + "/avaliacao",
            "{\"aprovado\":true,\"parecer\":\"Aprovado\"}", "empresa").statusCode()).isEqualTo(409);
        avaliar(pendenteSobreposto, "empresa", false);
        criarPedido(segundo, automovelId, retirada.plusDays(3), retirada.plusDays(5));

        var exclusao = enviar("DELETE", "/api/automoveis/" + automovelId, null, "empresa");
        assertThat(exclusao.statusCode()).isEqualTo(409);
        assertThat(exclusao.body()).contains("possui pedidos");
    }

    @Test
    void pedidoComRetiradaVencidaSoPodeSerRejeitado() throws Exception {
        String cliente = cadastrarCliente();
        String automovelId = cadastrarAutomovel("QRS7T89");
        LocalDate retirada = LocalDate.now().plusDays(5);
        String aprovadoId = criarPedido(cliente, automovelId, retirada, retirada.plusDays(2));
        avaliar(aprovadoId, "empresa", true);
        String pendenteId = criarPedido(cliente, automovelId, retirada.plusDays(10), retirada.plusDays(12));
        vencer(aprovadoId);
        vencer(pendenteId);

        var contrato = enviar("POST", "/api/contratos",
            "{\"pedidoId\":\"%s\",\"tipoPropriedade\":\"CLIENTE\"}".formatted(aprovadoId), cliente);
        assertThat(contrato.statusCode()).isEqualTo(409);
        assertThat(contrato.body()).contains("já passou");
        var aprovacao = enviar("POST", "/api/pedidos/" + pendenteId + "/avaliacao",
            "{\"aprovado\":true,\"parecer\":\"Aprovado\"}", "empresa");
        assertThat(aprovacao.statusCode()).isEqualTo(409);
        assertThat(aprovacao.body()).contains("só pode ser rejeitado");
        avaliar(pendenteId, "empresa", false);
    }

    private void vencer(String pedidoId) {
        Pedido pedido = pedidos.findById(UUID.fromString(pedidoId)).orElseThrow();
        pedido.modificar(pedido.getAutomovel(), LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        pedidos.saveAndFlush(pedido);
    }

    @Test
    void contratoPodeRegistrarBancoComoProprietario() throws Exception {
        String cliente = cadastrarCliente();
        String banco = "b" + UUID.randomUUID().toString().replace("-", "");
        usuarios.saveAndFlush(new Usuario("Banco", banco, codificador.encode("Senha@123"), Perfil.BANCO));
        String automovelId = cadastrarAutomovel("MNO4P56");
        String outroAutomovel = "{\"matricula\":\"MATBANCO1\",\"ano\":2025,\"marca\":\"Fiat\",\"modelo\":\"Mobi\",\"placa\":\"BNC1A23\"}";
        assertThat(enviar("POST", "/api/automoveis", outroAutomovel, banco).statusCode()).isEqualTo(403);
        assertThat(enviar("PUT", "/api/automoveis/" + automovelId, outroAutomovel, banco).statusCode()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/automoveis/" + automovelId, null, banco).statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/api/automoveis", null, banco).statusCode()).isEqualTo(200);
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
        String cpf = Cpfs.gerar();
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

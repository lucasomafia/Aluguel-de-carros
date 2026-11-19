package br.com.aluguel;

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
class ClienteIntegrationTest {
    @Value("${local.server.port}") int porta;
    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    @Test
    void criaConsultaAtualizaEExcluiPropriosDados() throws Exception {
        String nomeUsuario = "c" + UUID.randomUUID().toString().replace("-", "");
        String cpf = Cpfs.gerar();
        assertThat(enviar("POST", "/api/auth/cadastro", "{\"nome\":\"Ana\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeUsuario), null).statusCode()).isEqualTo(201);
        assertThat(enviar("GET", "/api/clientes/me", null, nomeUsuario).statusCode()).isEqualTo(404);
        String dados = "{\"rg\":\"12345\",\"cpf\":\"%s\",\"endereco\":\"Rua A\",\"profissao\":\"Professora\",\"vinculos\":[{\"entidadeEmpregadora\":\"Escola\",\"rendimento\":2500.50}]}".formatted(cpf);
        var criado = enviar("PUT", "/api/clientes/me", dados, nomeUsuario);
        assertThat(criado.statusCode()).isEqualTo(200);
        assertThat(criado.body()).contains("Ana", "Escola", "2500.50");
        assertThat(enviar("GET", "/api/clientes/me", null, nomeUsuario).body()).contains("Professora", cpf);
        String alterado = dados.replace("Rua A", "Rua B").replace("2500.50", "3000.00");
        assertThat(enviar("PUT", "/api/clientes/me", alterado, nomeUsuario).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/api/clientes/me", null, nomeUsuario).body()).contains("Rua B", "3000.00");
        assertThat(enviar("DELETE", "/api/clientes/me", null, nomeUsuario).statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/api/clientes/me", null, nomeUsuario).statusCode()).isEqualTo(404);
        assertThat(enviar("GET", "/api/auth/me", null, nomeUsuario).statusCode()).isEqualTo(200);
    }

    @Test
    void restringeAcessoEValidaLimiteDeVinculos() throws Exception {
        assertThat(enviar("GET", "/api/clientes/me", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/clientes/me", null, "empresa").statusCode()).isEqualTo(403);
        String nomeUsuario = "c" + UUID.randomUUID().toString().replace("-", "");
        enviar("POST", "/api/auth/cadastro", "{\"nome\":\"Ana\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeUsuario), null);
        String vinculo = "{\"entidadeEmpregadora\":\"Empresa\",\"rendimento\":1000}";
        String dados = "{\"rg\":\"1\",\"cpf\":\"%s\",\"endereco\":\"Rua\",\"profissao\":\"Cargo\",\"vinculos\":[%s,%s,%s,%s]}".formatted(Cpfs.gerar(), vinculo, vinculo, vinculo, vinculo);
        assertThat(enviar("PUT", "/api/clientes/me", dados, nomeUsuario).statusCode()).isEqualTo(400);
    }

    @Test
    void recusaCpfComDigitosVerificadoresInvalidos() throws Exception {
        String nomeUsuario = "c" + UUID.randomUUID().toString().replace("-", "");
        enviar("POST", "/api/auth/cadastro", "{\"nome\":\"Ana\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\"}".formatted(nomeUsuario), null);
        String dados = "{\"rg\":\"1\",\"cpf\":\"%s\",\"endereco\":\"Rua\",\"profissao\":\"Cargo\",\"vinculos\":[]}";
        var invalido = enviar("PUT", "/api/clientes/me", dados.formatted("12345678901"), nomeUsuario);
        assertThat(invalido.statusCode()).isEqualTo(400);
        assertThat(invalido.body()).contains("CPF inválido");
        assertThat(enviar("PUT", "/api/clientes/me", dados.formatted("00000000000"), nomeUsuario).statusCode()).isEqualTo(400);
        assertThat(enviar("PUT", "/api/clientes/me", dados.formatted("52998224725"), nomeUsuario).statusCode()).isEqualTo(200);
    }

    @Test
    void cadastroDeContaJaGravaDadosDoClienteOuNadaEmCasoDeErro() throws Exception {
        String nomeUsuario = "c" + UUID.randomUUID().toString().replace("-", "");
        String cpf = Cpfs.gerar();
        String conta = "{\"nome\":\"Bia\",\"nomeUsuario\":\"%s\",\"senha\":\"Senha@123\",\"cliente\":{\"rg\":\"77\",\"cpf\":\"%s\",\"endereco\":\"Rua C\",\"profissao\":\"Médica\",\"vinculos\":[{\"entidadeEmpregadora\":\"Hospital\",\"rendimento\":9000}]}}";
        assertThat(enviar("POST", "/api/auth/cadastro", conta.formatted(nomeUsuario, cpf), null).statusCode()).isEqualTo(201);
        assertThat(enviar("GET", "/api/clientes/me", null, nomeUsuario).body()).contains("Médica", "Hospital", cpf);

        String outro = "c" + UUID.randomUUID().toString().replace("-", "");
        var duplicado = enviar("POST", "/api/auth/cadastro", conta.formatted(outro, cpf), null);
        assertThat(duplicado.statusCode()).isEqualTo(409);
        assertThat(duplicado.body()).contains("CPF já cadastrado");
        assertThat(enviar("POST", "/api/auth/cadastro", conta.formatted(outro, "12345678901"), null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/api/auth/me", null, outro).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String nomeUsuario) throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/json");
        if (nomeUsuario != null) {
            String senha = nomeUsuario.equals("empresa") ? "Empresa@123" : "Senha@123";
            requisicao.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((nomeUsuario + ":" + senha).getBytes(StandardCharsets.UTF_8)));
        }
        requisicao.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        return clienteHttp.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }
}

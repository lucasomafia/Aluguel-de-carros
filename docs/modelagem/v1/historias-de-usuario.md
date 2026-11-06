# Histórias de usuário — versão 1

## HU01 — Acesso ao sistema

Como usuário, quero me cadastrar e entrar com usuário e senha para acessar o sistema com meu perfil de cliente ou agente.

Critérios de aceitação:

- O cadastro público cria contas de cliente; empresas e bancos são provisionados pelo responsável pelo sistema.
- Credenciais corretas permitem acesso; credenciais incorretas são rejeitadas.
- Senhas são armazenadas com BCrypt e não são retornadas pela API.
- Somente usuários autenticados acessam as funcionalidades protegidas.

Situação: implementada.

## HU02 — Cadastro do cliente e pedidos de aluguel

Como cliente, quero manter meus dados e criar, consultar, modificar e cancelar meus pedidos para gerenciar meus aluguéis.

Critérios de aceitação:

- Registrar RG, CPF, nome, endereço, profissão e até três entidades empregadoras com os rendimentos.
- Permitir escolher um automóvel identificado por matrícula, ano, marca, modelo e placa.
- Exibir o status dos pedidos do cliente autenticado.
- Restringir alterações e cancelamentos aos pedidos do próprio cliente, conforme sua situação.

Situação: prevista.

## HU03 — Avaliação e contratação

Como agente de empresa ou banco, quero modificar e avaliar financeiramente os pedidos para formalizar os aluguéis aprovados.

Critérios de aceitação:

- Registrar o agente responsável e o parecer financeiro.
- Permitir formalização do contrato somente após aprovação financeira.
- Registrar a propriedade do automóvel conforme o contrato: cliente, empresa ou banco.
- Permitir crédito opcional concedido por um banco agente e associado ao aluguel.

Situação: prevista.

# Histórias de usuário — versão 2

Esta revisão mantém as três histórias definidas para o projeto e atualiza seus critérios conforme os fluxos implementados.

Wireframes das telas: [abrir no Figma](https://www.figma.com/design/5cT6LQGGLK7wobxhB88722?node-id=3-2).

## HU01 — Acessar o sistema

**Como** cliente ou agente (empresa ou banco), **quero** entrar com usuário e senha, **para** acessar as funções permitidas ao meu perfil.

**Critérios de aceitação**

- O cadastro público cria uma conta de cliente.
- Contas de agentes possuem perfil de empresa ou banco.
- Usuário e senha válidos autenticam; credenciais inválidas não dão acesso.
- As operações protegidas exigem autenticação e respeitam o perfil.

**Situação:** implementada.

## HU02 — Cliente gerencia seus dados e pedidos

**Como** cliente autenticado, **quero** manter meus dados e rendimentos, consultar automóveis e gerenciar pedidos de aluguel, **para** acompanhar a solicitação até a contratação.

**Critérios de aceitação**

- O cliente cadastra e atualiza identificação, endereço e profissão.
- Pode informar até três entidades empregadoras e rendimentos.
- Pode consultar automóveis disponíveis.
- Pode criar, consultar, modificar e cancelar seus próprios pedidos, respeitando o estado do pedido.
- Pode executar o contrato depois da aprovação e indicar a propriedade para si ou para o agente que aprovou.
- O status do pedido é apresentado ao cliente.

**Situação:** implementada.

## HU03 — Agente analisa pedidos e banco registra crédito

**Como** agente autenticado, **quero** consultar e avaliar pedidos de aluguel, **para** registrar um parecer financeiro e encaminhar os aprovados à execução do contrato; **como** banco, **quero** vincular um crédito opcional ao contrato executado.

**Critérios de aceitação**

- Empresa e banco consultam pedidos e registram parecer e decisão.
- O pedido aprovado aguarda a execução do contrato pelo cliente.
- O contrato registra o automóvel, proprietário e data de execução, atualiza o estado do pedido e a propriedade atual do automóvel.
- Banco pode conceder um crédito por contrato executado, informando valor e de 1 a 120 parcelas.
- Banco consulta créditos que concedeu; outras instituições não podem registrar crédito.

**Situação:** implementada, sem cálculo de juros ou cronograma de pagamentos.

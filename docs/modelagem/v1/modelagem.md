# Modelagem do Sistema de Aluguel de Carros — versão 1

Professor: João Paulo Carneiro Aramuni.

## Escopo

Esta versão representa o sistema solicitado no enunciado. Os elementos marcados como implementados correspondem ao código atual; os demais orientam as próximas etapas. O README da raiz permanece vazio.

## Artefatos

- `casos-de-uso.puml`: atores, funções e fronteira do sistema.
- `historias-de-usuario.md`: histórias, critérios de aceitação e situação atual.
- `classes.puml`: entidades, atributos, associações e multiplicidades.
- `pacotes.puml`: visão lógica dos dois subsistemas e suas dependências.

Os arquivos `.puml` são fontes UML editáveis em PlantUML. O quadro editável está no [Figma/FigJam](https://www.figma.com/board/m31E6Me5jr4bv18Z9QXWx1). Os arquivos locais preservam as fontes UML e as três histórias de usuário para versionamento no GitHub.

## Correspondência com o código atual

| Modelo | Código existente |
| --- | --- |
| Conta de acesso | `Usuario`, `Perfil`, `UsuarioRepository` |
| Cadastro de conta | `AuthController`, `CadastroRequest`, `UsuarioResponse`, `UsuarioService` |
| Autenticação | `SecurityConfig`, `UsuarioService`, HTTP Basic, BCrypt |
| Provisionamento de agentes | `AgenteInicialConfig` |
| Persistência | Tabela `usuarios` no Supabase, migração Flyway V1 |
| Apresentação | `src/App.jsx`, `src/index.css` |

`Cliente` é um cadastro de domínio, distinto da conta `Usuario`. O nome será obtido de `Usuario.nome`, evitando dois nomes divergentes. O código atual ainda não tem CPF, RG, endereço, profissão nem fontes de renda.

## Regras extraídas do enunciado

1. As funções do sistema exigem cadastro e autenticação; o cadastro da conta é a porta de entrada pública.
2. Clientes introduzem, modificam, consultam e cancelam seus pedidos.
3. Agentes, com perfil EMPRESA ou BANCO, modificam e avaliam pedidos.
4. Somente parecer financeiro positivo permite prosseguir para a contratação.
5. O cliente possui no máximo três vínculos de emprego/rendimento.
6. Automóveis possuem matrícula, ano, marca, modelo e placa.
7. A propriedade do veículo pode pertencer a cliente, empresa ou banco conforme o contrato.
8. Crédito é opcional e deve ter um banco agente como concedente.
9. A apresentação web e a gestão de pedidos/contratos formam dois subsistemas lógicos, comunicados pela API.

## Decisões propostas para validar

- A relação conta–cliente e conta–agente é individual nesta versão. Vários funcionários por agente exigiriam uma evolução do modelo.
- Agentes não recebem privilégios por cadastro público; o responsável pelo sistema os provisiona.
- CPF, matrícula e placa deverão ser únicos; a validação documental será implementada no CRUD.
- A entidade `Agente` complementará as contas EMPRESA/BANCO com a identificação da organização. A restrição BANCO no crédito deve ser validada pelo serviço.
- Novos pedidos começam em EM_ANALISE. Avaliação positiva leva a APROVADO; negativa, a REPROVADO. Cancelamento leva a CANCELADO; formalização leva a CONTRATADO.
- Modificações que alterem condições financeiras antes da contratação invalidam a aprovação e exigem nova avaliação.
- Não se altera nem cancela diretamente um pedido já contratado; rescisão contratual não foi detalhada no enunciado.
- Datas, valor total, número do crédito e parcelas são dados adicionais propostos para tornar pedidos e contratos operacionais.
- O proprietário atual do automóvel e o proprietário definido no contrato são referências explícitas; a atualização da propriedade dependerá da formalização e do tipo contratual.
- O limite de três vínculos deve ser garantido no serviço, com proteção contra operações concorrentes; apenas a cardinalidade do desenho não implementa a regra.

## Controle de versões

Esta é a primeira versão da modelagem. Revisões futuras devem ser salvas em outra pasta de versão, registrando as alterações e sua relação com o código. O desenho não significa que todas as funcionalidades já estejam implementadas.

# Modelagem do sistema — versão 2

Esta versão atualiza os diagramas e histórias para refletir os fluxos presentes no código após a implementação de pedidos, avaliação financeira, contratos e crédito bancário. A versão anterior foi removida do projeto.

## Artefatos

- [Wireframes editáveis no Figma](https://www.figma.com/design/5cT6LQGGLK7wobxhB88722?node-id=3-2)
- [Guia de acesso aos wireframes](../../wireframes.docx)
- [Casos de uso](casos-de-uso.puml)
- [Diagrama de classes](classes.puml)
- [Diagrama de pacotes](pacotes.puml)
- [Histórias de usuário](historias-de-usuario.md)

Os arquivos `.puml` podem ser renderizados com PlantUML.

## Alinhamento com a implementação

- A visão de classes representa as entidades `Usuario`, `Cliente`, `VinculoEmpregaticio`, `Automovel`, `Pedido`, `Contrato` e `ContratoCredito`, além dos enums de perfil, estado e propriedade.
- O cadastro público é de clientes. Os agentes operam com perfil `EMPRESA` ou `BANCO`.
- Cliente pode gerenciar seus dados e até três vínculos empregatícios, consultar automóveis e gerenciar os próprios pedidos.
- Agentes analisam e avaliam pedidos. Um pedido aprovado aguarda o cliente executar o contrato.
- O contrato define o proprietário do automóvel como cliente ou agente que aprovou o pedido e atualiza a propriedade atual do veículo.
- Banco pode registrar no máximo um crédito por contrato, com valor e quantidade de parcelas entre 1 e 120. Esta versão não modela juros nem pagamentos das parcelas.
- A visão de pacotes representa a interface React/Vite, API Spring MVC, camadas de serviço e repositório, modelos e persistência Supabase/PostgreSQL com migrações Flyway.

## Mudanças em relação à versão 1

- Inclusão do fluxo de contrato como etapa após parecer financeiro positivo.
- Inclusão das alternativas de proprietário do automóvel e atualização da propriedade após contrato.
- Inclusão do crédito opcional concedido pelo banco e associado ao contrato.
- Atualização dos pacotes e componentes para refletir as classes e camadas implementadas.
- Revisão das três histórias e dos critérios de aceitação para corresponder ao comportamento atual.

## Limites do escopo modelado

O diagrama descreve o funcionamento existente. Cálculo de juros, pagamentos e cronograma de parcelas, além de processos administrativos adicionais, não estão representados como funcionalidades implementadas.

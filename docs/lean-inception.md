# Lean Inception do Pulso

Workshop de alinhamento feito antes do código para decidir **o que construir primeiro** e **o que deixar de fora**.

## Visão do produto

**Para** diretores e gerentes de redes de academias
**que** descobrem tarde demais que um aluno parou de treinar e que a mensalidade vai ser cancelada,
**o Pulso** é um sistema de gestão
**que** transforma catraca, cobrança e agenda de aulas em um aviso antecipado de evasão e em um painel executivo da rede.
**Diferente de** planilhas e sistemas que só cadastram aluno e emitem boleto,
**nosso produto** mostra quem está em risco, por quê, e quanto de receita está em jogo.

## O produto é / não é / faz / não faz

| É | Não é |
|---|---|
| Gestão de rede com várias unidades | Aplicativo de treino (séries e cargas) |
| Painel executivo e operação da recepção | Gateway de pagamento |
| Aviso de risco com motivos explicados | Caixa-preta de "inteligência artificial" |

| Faz | Não faz (por enquanto) |
|---|---|
| Matrícula, planos, mensalidades e baixa de pagamento | Emissão real de boleto/Pix |
| Catraca com regras (plano, unidade, atraso, lotação) | Integração com hardware de catraca |
| Aulas com reserva e fila de espera | Cobrança recorrente em cartão |
| Avaliação física com IMC e evolução | Prescrição de treino |

## Personas

- **Marina, diretora da rede.** Abre o painel no celular entre reuniões. Quer saber em dez segundos se a receita está saudável e onde agir.
- **Rogério, gerente de unidade.** Quer a lista de quem está sumindo para ligar hoje e quem está devendo.
- **Camila, recepcionista.** Precisa liberar ou negar a catraca e dizer o motivo sem consultar ninguém.
- **Bruno, aluno.** Quer reservar aula e, se estiver cheia, entrar na fila sem perder a vaga quando alguém cancelar.

## Jornadas

1. **Marina:** abre o painel → vê MRR, inadimplência, cancelamentos e receita em risco → filtra pela unidade com mais alertas → abre o ranking de risco.
2. **Rogério:** abre o risco da unidade → escolhe o aluno de maior nota → lê os motivos (18 dias sem treinar, mensalidade vencida) → registra o contato e o pagamento.
3. **Camila:** digita o CPF → catraca libera ou nega com o motivo → o painel "na unidade agora" se atualiza.
4. **Bruno:** escolhe o dia → reserva → se cheia, entra na fila → alguém cancela e ele é promovido.

## Funcionalidades, valor e esforço

| Funcionalidade | Valor para o negócio | Esforço | Onda |
|---|---|---|---|
| Cadastro de alunos, planos e matrículas | Base de tudo | Médio | 1 |
| Mensalidades e baixa de pagamento (Pix, cartão, boleto) | Receita visível | Médio | 1 |
| Avaliação física com IMC (o desafio original) | Acompanhamento do aluno | Baixo | 1 |
| Painel executivo (MRR, inadimplência, churn, ocupação) | Decisão da diretoria | Alto | 2 |
| Catraca com regras e ocupação ao vivo | Operação e segurança | Médio | 2 |
| Risco de evasão com motivos | **Diferencial:** protege receita | Alto | 2 |
| Aulas com reserva, fila de espera e bloqueio otimista | Experiência do aluno | Alto | 3 |
| Laboratório de JPA (N+1 e concorrência medidos) | Prova técnica | Médio | 3 |
| Acesso pelo celular com QR Code e instalação como app | Diretoria em movimento | Baixo | 3 |

## Sequenciador

- **Onda 1 · fundamentos:** modelo de dados no JPA, CRUD de alunos, matrícula, mensalidades, avaliações. *Critério de pronto: dá para matricular e cobrar um aluno de ponta a ponta.*
- **Onda 2 · inteligência e operação:** catraca, painel executivo e risco de evasão. *Critério de pronto: a diretora responde "quanto de receita está em risco e onde?" em dez segundos.*
- **Onda 3 · experiência e prova técnica:** aulas com fila, laboratório de JPA, celular. *Critério de pronto: 40 threads disputando 3 vagas sem overbooking, comprovado por teste.*
- **Próximas ondas:** login com perfis (diretoria, gerente, recepção, aluno), cobrança recorrente real, notificações para quem está em risco, multi-tenant por rede.

## Canvas do MVP

- **Proposta de valor:** saber quem vai cancelar antes de cancelar.
- **Segmentos:** redes de 3 a 50 unidades.
- **Resultados esperados:** reduzir cancelamentos evitáveis e inadimplência; dar visão única da rede.
- **Métricas de sucesso:** receita recuperada de alunos de risco alto contatados; tempo para a diretoria responder "como está a rede?"; reservas sem overbooking.
- **Custos:** só infraestrutura (Java + banco); sem dependência paga.
- **Riscos:** nota de risco mal calibrada gerar ruído (mitigado com motivos explícitos e testes da regra); concorrência em aulas (mitigado com `@Version` e teste com threads reais).
- **Cronograma:** 3 ondas, cada uma entregável e testada.

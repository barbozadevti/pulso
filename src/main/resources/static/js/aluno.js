// Ficha do aluno: tudo numa chamada (matrículas, cobranças, avaliações, frequência, risco e reservas).
import { h, icone, limpar, reais, data, dataCurta, dataHora, iniciais, modal, toast, esqueleto, erroNaTela, hojeISO } from './ui.js';
import { api, referencias } from './api.js';
import { barrasSemanais, linha } from './charts.js';
import { rotuloNivel } from './painel.js';
import { registrarContato, CANAIS, RESULTADOS } from './contato.js';

export async function renderizar(raiz, { id }) {
  async function carregar() {
    const f = await api.get(`/api/alunos/${id}`);
    desenhar(f);
  }

  function desenhar(f) {
    const a = f.aluno;
    const vigente = f.matriculas.find((m) => m.status !== 'CANCELADA');
    limpar(raiz).append(
      h('a', { class: 'botao pequeno', href: '#/alunos', style: 'margin-bottom:14px;display:inline-flex' }, icone('voltar', 16), 'Alunos'),
      cabecalho(f, vigente),
      h('div', { class: 'grade-2', style: 'margin-top:16px' },
        h('div', { class: 'pilha' }, blocoRisco(f.risco, vigente), blocoRetencao(f), blocoFrequencia(f.frequencia), blocoReservas(f.reservas)),
        h('div', { class: 'pilha' }, blocoCobrancas(f.mensalidades), blocoMatriculas(f.matriculas))),
      blocoAvaliacoes(f.avaliacoes));
  }

  const acao = (texto, rota, corpo, msg, extra = '') => h('button', { class: 'botao pequeno ' + extra, type: 'button', onclick: async () => {
    try { await api.post(rota, corpo); toast(msg); await carregar(); } catch (e) { toast(e.message, 'erro'); }
  } }, texto);

  function cabecalho(f, vigente) {
    const a = f.aluno;
    const botoes = [];
    if (!vigente) botoes.push(h('button', { class: 'botao primario pequeno', type: 'button', onclick: () => matricular(a) }, 'Matricular'));
    else {
      botoes.push(h('button', { class: 'botao pequeno', type: 'button', onclick: () => trocarPlano(vigente) }, 'Mudar plano'));
      botoes.push(vigente.status === 'ATIVA' ? acao('Trancar', `/api/matriculas/${vigente.id}/trancar`, {}, 'Matrícula trancada.')
        : acao('Reativar', `/api/matriculas/${vigente.id}/reativar`, {}, 'Matrícula reativada.'));
      botoes.push(h('button', { class: 'botao pequeno perigo', type: 'button', onclick: () => cancelar(vigente) }, 'Cancelar matrícula'));
    }
    const selo = !vigente ? ['', 'Sem matrícula'] : vigente.status === 'ATIVA' ? ['ok', 'Ativo'] : ['atencao', 'Trancado'];
    return h('section', { class: 'cartao' },
      h('div', { class: 'ficha-topo' }, h('span', { class: 'avatar grande' }, iniciais(a.nome)),
        h('div', { class: 'info' }, h('h1', {}, a.nome), h('p', {}, `${a.email} · ${a.cpf}`),
          h('p', {}, `${a.bairro}, ${a.cidade}/${a.uf} · ${a.idade} anos`)),
        h('div', { class: 'acoes', style: 'display:flex;gap:8px;flex-wrap:wrap' }, botoes)),
      h('div', { class: 'linha-dados' },
        dado('Situação', h('span', { class: 'selo ' + selo[0] }, selo[1])),
        dado('Plano', vigente ? `${vigente.plano} · ${reais(vigente.valor)}` : '—'),
        dado('Unidade', a.unidade.replace('Pulso ', '')),
        dado('Aluno desde', data(f.matriculas.length ? f.matriculas[f.matriculas.length - 1].inicio : null))));
  }

  async function matricular(a) {
    const { planos } = await referencias();
    const sel = h('select', { class: 'entrada' }, planos.map((p) => h('option', { value: p.id }, `${p.nome} · ${reais(p.valorMensal)}`)));
    const m = modal('Matricular ' + a.nome.split(' ')[0], h('div', { class: 'pilha' },
      h('label', { class: 'campo' }, h('span', {}, 'Plano'), sel), planosDetalhe(planos, sel),
      h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', onclick: () => m.fechar() }, 'Cancelar'),
        h('button', { class: 'botao primario', onclick: async () => {
          try { await api.post('/api/matriculas', { alunoId: Number(id), planoId: Number(sel.value) }); m.fechar(); toast('Matriculado. Primeira mensalidade gerada.'); await carregar(); }
          catch (e) { toast(e.message, 'erro'); }
        } }, 'Confirmar'))));
  }

  async function trocarPlano(vigente) {
    const { planos } = await referencias();
    const sel = h('select', { class: 'entrada' }, planos.map((p) => h('option', { value: p.id, selected: p.id === vigente.planoId }, `${p.nome} · ${reais(p.valorMensal)}`)));
    const m = modal('Mudar de plano', h('div', { class: 'pilha' },
      h('label', { class: 'campo' }, h('span', {}, 'Novo plano'), sel), planosDetalhe(planos, sel),
      h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', onclick: () => m.fechar() }, 'Cancelar'),
        h('button', { class: 'botao primario', onclick: async () => {
          try { await api.post(`/api/matriculas/${vigente.id}/plano`, { planoId: Number(sel.value) }); m.fechar(); toast('Plano alterado. Vale a partir da próxima mensalidade.'); await carregar(); }
          catch (e) { toast(e.message, 'erro'); }
        } }, 'Salvar'))));
  }

  function cancelar(vigente) {
    const motivo = h('select', { class: 'entrada' }, ['Preço', 'Falta de tempo', 'Mudança de cidade', 'Lesão', 'Foi para outra academia', 'Não informado'].map((x) => h('option', {}, x)));
    const m = modal('Cancelar matrícula', h('div', { class: 'pilha' }, h('p', {}, 'O aluno deixa de treinar e a catraca será bloqueada. Dá para matricular de novo depois.'),
      h('label', { class: 'campo' }, h('span', {}, 'Motivo'), motivo),
      h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', onclick: () => m.fechar() }, 'Voltar'),
        h('button', { class: 'botao perigo', onclick: async () => {
          try { await api.post(`/api/matriculas/${vigente.id}/cancelar`, { motivo: motivo.value }); m.fechar(); toast('Matrícula cancelada.'); await carregar(); }
          catch (e) { toast(e.message, 'erro'); }
        } }, 'Cancelar matrícula'))));
  }

  function blocoRisco(r, vigente) {
    if (!vigente) return null;
    if (!r) return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Risco de evasão')), h('p', { class: 'mudo' }, 'Matrícula trancada: sem avaliação de risco.'));
    return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Risco de evasão'), h('span', { class: 'risco ' + r.nivel }, `${r.pontos} · ${rotuloNivel(r.nivel)}`)),
      r.fatores.length ? h('div', { class: 'fatores' }, r.fatores.map((x) => h('span', { class: 'selo' }, x))) : h('p', { class: 'suave' }, 'Frequência e pagamentos em dia. Nenhum sinal de risco.'));
  }

  function blocoRetencao(f) {
    return h('section', { class: 'cartao' },
      h('header', {}, h('h2', {}, 'Retenção'), h('button', { class: 'botao pequeno primario', type: 'button',
        onclick: () => registrarContato({ alunoId: id, nome: f.aluno.nome, aoSalvar: carregar }) }, icone('mais', 16), 'Registrar contato')),
      f.contatos.length ? h('div', { class: 'lista' }, f.contatos.map((c) => h('div', { class: 'item' },
        h('span', { class: 'corpo' }, h('strong', {}, `${CANAIS[c.canal]} · ${dataHora(c.feitoEm)}`),
          h('span', {}, `${c.observacao ? c.observacao + ' · ' : ''}risco na época: ${c.riscoNaEpoca}`)),
        h('span', { class: 'selo ' + RESULTADOS[c.resultado][0] }, RESULTADOS[c.resultado][1]))))
        : h('p', { class: 'mudo' }, 'Nenhum contato registrado.'));
  }

  function blocoFrequencia(fr) {
    return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Frequência'), h('p', {}, 'últimas 8 semanas')),
      h('div', { class: 'linha-dados', style: 'margin:0 0 10px' }, dado('Visitas em 30 dias', String(fr.visitas30Dias)),
        dado('Último treino', fr.ultimaVisita ? `${dataHora(fr.ultimaVisita)}` : 'nenhum')),
      barrasSemanais(fr.porSemana));
  }

  function blocoReservas(rs) {
    return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Próximas aulas')),
      rs.length ? h('div', { class: 'lista' }, rs.map((r) => h('div', { class: 'item' }, h('span', { style: 'font-size:1.4rem' }, r.icone),
        h('span', { class: 'corpo' }, h('strong', {}, r.modalidade), h('span', {}, `${dataCurta(r.inicio)} às ${r.inicio.slice(11, 16)} · ${r.unidade.replace('Pulso ', '')}`)),
        h('span', { class: 'fim' }, h('span', { class: 'selo ' + (r.status === 'CONFIRMADA' ? 'ok' : 'atencao') }, r.status === 'CONFIRMADA' ? 'Confirmada' : 'Fila de espera'),
          h('button', { class: 'icone-botao', 'aria-label': 'Cancelar reserva', onclick: async () => {
            try { const c = await api.apagar(`/api/reservas/${r.id}`); toast(c.promoveuAlguem ? `Cancelada. ${c.promovido} saiu da fila e entrou na aula.` : 'Reserva cancelada.'); await carregar(); }
            catch (e) { toast(e.message, 'erro'); }
          } }, icone('fechar', 18))))))
        : h('p', { class: 'mudo' }, 'Nenhuma reserva. Reserve em Aulas.'));
  }

  function blocoCobrancas(ms) {
    const rotulo = { PAGA: ['ok', 'Paga'], ABERTA: ['', 'Em aberto'], ATRASADA: ['critico', 'Atrasada'] };
    return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Mensalidades')),
      ms.length ? h('div', { class: 'rolagem-x' }, h('table', { class: 'tabela' },
        h('thead', {}, h('tr', {}, ['Mês', 'Valor', 'Situação', ''].map((t, i) => h('th', { class: i > 0 && i % 3 === 0 ? 'dir' : '' }, t)))),
        h('tbody', {}, ms.map((m) => h('tr', {}, h('td', {}, dataCurta(m.competencia) + ' ' + m.competencia.slice(0, 4)), h('td', { class: 'num' }, reais(m.valor)),
          h('td', {}, h('span', { class: 'selo ' + rotulo[m.situacao][0] }, rotulo[m.situacao][1] + (m.diasDeAtraso ? ` · ${m.diasDeAtraso}d` : '')),
            m.forma ? h('div', { class: 'mudo', style: 'font-size:.75rem;margin-top:3px' }, `${m.forma.toLowerCase()} · ${m.referencia}`) : h('div', { class: 'mudo', style: 'font-size:.75rem;margin-top:3px' }, `vence ${data(m.vencimento)}`)),
          h('td', { class: 'dir' }, m.situacao === 'PAGA' ? null : h('button', { class: 'botao pequeno primario', onclick: () => pagar(m) }, 'Receber'))))))) : h('p', { class: 'mudo' }, 'Sem mensalidades.'));
  }

  function pagar(m) {
    const forma = h('select', { class: 'entrada' }, [['PIX', 'Pix'], ['CARTAO', 'Cartão'], ['BOLETO', 'Boleto']].map(([v, t]) => h('option', { value: v }, t)));
    const ref = h('input', { class: 'entrada', placeholder: 'Final do cartão ou código (opcional)' });
    const mm = modal(`Receber ${dataCurta(m.competencia)}`, h('div', { class: 'pilha' }, h('p', {}, `${reais(m.valor)} · vencimento ${data(m.vencimento)}`),
      h('label', { class: 'campo' }, h('span', {}, 'Forma de pagamento'), forma), h('label', { class: 'campo' }, h('span', {}, 'Referência'), ref),
      h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', onclick: () => mm.fechar() }, 'Cancelar'),
        h('button', { class: 'botao primario', onclick: async () => {
          try { await api.post(`/api/mensalidades/${m.id}/pagar`, { forma: forma.value, referencia: ref.value || null }); mm.fechar(); toast('Pagamento registrado.'); await carregar(); }
          catch (e) { toast(e.message, 'erro'); }
        } }, 'Registrar pagamento'))));
  }

  function blocoMatriculas(ms) {
    return h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Histórico de matrículas')),
      h('div', { class: 'lista' }, ms.map((m) => h('div', { class: 'item' },
        h('span', { class: 'corpo' }, h('strong', {}, `${m.plano} · ${reais(m.valor)}`), h('span', {}, `${data(m.inicio)} → ${m.fim ? data(m.fim) : 'hoje'}${m.motivoCancelamento ? ' · ' + m.motivoCancelamento : ''}`)),
        h('span', { class: 'selo ' + (m.status === 'ATIVA' ? 'ok' : m.status === 'TRANCADA' ? 'atencao' : '') }, m.status.toLowerCase())))));
  }

  function blocoAvaliacoes(av) {
    const ult = av[av.length - 1], prim = av[0];
    const delta = (campo, un) => (av.length > 1 && ult[campo] != null && prim[campo] != null)
      ? `${(ult[campo] - prim[campo] > 0 ? '+' : '')}${(ult[campo] - prim[campo]).toFixed(1).replace('.', ',')} ${un} desde ${dataCurta(prim.data)}` : '';
    return h('section', { class: 'cartao', style: 'margin-top:16px' },
      h('header', {}, h('h2', {}, 'Avaliações físicas'), h('button', { class: 'botao pequeno primario', onclick: () => novaAvaliacao() }, icone('mais', 16), 'Nova avaliação')),
      av.length ? h('div', { class: 'grade-3' },
        h('div', {}, h('div', { class: 'linha-dados', style: 'margin:0 0 6px' }, dado('Peso', `${ult.peso.toFixed(1).replace('.', ',')} kg`), dado('IMC', `${String(ult.imc).replace('.', ',')} · ${ult.faixa}`)),
          h('p', { class: 'suave' }, delta('peso', 'kg')), linha(av.map((x) => ({ rotulo: dataCurta(x.data), y: x.peso })), { cor: 's1', unidade: 'kg' })),
        h('div', {}, h('div', { class: 'linha-dados', style: 'margin:0 0 6px' }, dado('Gordura corporal', ult.percentualGordura != null ? `${ult.percentualGordura.toFixed(1).replace('.', ',')}%` : '—')),
          h('p', { class: 'suave' }, delta('percentualGordura', 'p.p.')), linha(av.filter((x) => x.percentualGordura != null).map((x) => ({ rotulo: dataCurta(x.data), y: x.percentualGordura })), { cor: 's2', unidade: '%' })),
        h('div', {}, h('div', { class: 'linha-dados', style: 'margin:0 0 6px' }, dado('Cintura', ult.cinturaCm != null ? `${ult.cinturaCm.toFixed(1).replace('.', ',')} cm` : '—')),
          h('p', { class: 'suave' }, delta('cinturaCm', 'cm')), linha(av.filter((x) => x.cinturaCm != null).map((x) => ({ rotulo: dataCurta(x.data), y: x.cinturaCm })), { cor: 's3', unidade: 'cm' })))
        : h('p', { class: 'mudo' }, 'Nenhuma avaliação registrada ainda.'));
  }

  function novaAvaliacao() {
    const c = (rotulo, nome, atrib) => h('label', { class: 'campo' }, h('span', {}, rotulo), h('input', { class: 'entrada', name: nome, inputmode: 'decimal', ...atrib }));
    const form = h('form', { class: 'formulario' }, c('Data', 'data', { type: 'date', value: hojeISO(), required: true }),
      c('Altura (m)', 'altura', { placeholder: '1,75', required: true }), c('Peso (kg)', 'peso', { placeholder: '78,5', required: true }),
      c('Gordura (%)', 'percentualGordura', { placeholder: '20,0' }), c('Cintura (cm)', 'cinturaCm', { placeholder: '84,0' }));
    const erro = h('p', { role: 'alert', style: 'color:var(--critico);min-height:1.2em' });
    const m = modal('Nova avaliação física', h('div', { class: 'pilha' }, form, erro,
      h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', onclick: () => m.fechar() }, 'Cancelar'),
        h('button', { class: 'botao primario', onclick: () => form.requestSubmit() }, 'Salvar'))));
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const n = (v) => (v === '' || v == null ? null : Number(String(v).replace(',', '.')));
      const d = Object.fromEntries(new FormData(form));
      try {
        await api.post(`/api/alunos/${id}/avaliacoes`, { data: d.data, altura: n(d.altura), peso: n(d.peso), percentualGordura: n(d.percentualGordura), cinturaCm: n(d.cinturaCm) });
        m.fechar(); toast('Avaliação registrada.'); await carregar();
      } catch (ex) { erro.textContent = ex.message; }
    });
  }

  raiz.append(esqueleto(5));
  await carregar();
}

function dado(rotulo, valor) {
  return h('div', { class: 'dado' }, h('span', {}, rotulo), h('strong', {}, valor));
}

function planosDetalhe(planos, sel) {
  const caixa = h('div', { class: 'suave', style: 'font-size:.88rem' });
  const atualizar = () => {
    const p = planos.find((x) => String(x.id) === sel.value);
    caixa.textContent = `${p.modalidades.map((m) => m.icone + ' ' + m.nome).join(' · ')} — ${p.multiUnidade ? 'treina em todas as unidades' : 'só na unidade de origem'}.`;
  };
  sel.addEventListener('change', atualizar);
  atualizar();
  return caixa;
}

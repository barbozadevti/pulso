// Painel executivo: o que um CEO quer ver em dez segundos.
import { h, icone, limpar, reais, reaisInteiros, compacto, numero, pct, esqueleto, erroNaTela } from './ui.js';
import { api, consulta } from './api.js';
import { estado } from './estado.js';
import { barrasPorMes, mapaDeCalor, barrasHorizontais } from './charts.js';

export async function renderizar(raiz) {
  let temporizador;
  let vivo = true;

  async function carregar(primeira) {
    const p = await api.get('/api/painel' + consulta({ unidadeId: estado.unidadeId }));
    if (!vivo) return;
    desenhar(p);
    if (!primeira) raiz.classList.remove('carregando');
  }

  function desenhar(p) {
    const k = p.kpis;
    const unidade = p.unidades.find((u) => String(u.id) === estado.unidadeId);
    const ocupacao = k.capacidadeTotal ? Math.round((k.dentroAgora / k.capacidadeTotal) * 100) : 0;
    const maisRisco = [...p.unidades].sort((a, b) => b.riscoAlto - a.riscoAlto)[0];

    limpar(raiz).append(
      h('div', { class: 'topo' },
        h('div', {}, h('h1', {}, unidade ? unidade.nome : 'Visão da rede'),
          h('p', {}, resumo(p, unidade, maisRisco))),
        h('div', { class: 'acoes' }, h('span', { class: 'ao-vivo' }, 'ao vivo'))),

      h('section', { class: 'kpis', 'aria-label': 'Indicadores' },
        h('div', { class: 'kpi destaque' }, h('span', { class: 'rotulo' }, 'Receita recorrente (MRR)'),
          h('span', { class: 'valor' }, compacto(k.receitaRecorrente)), h('span', { class: 'nota' }, `${numero(k.ativos)} alunos ativos`)),
        h('a', { class: 'kpi alerta link', href: '#/risco', style: 'text-decoration:none;color:inherit' },
          h('span', { class: 'rotulo' }, 'Receita em risco'), h('span', { class: 'valor' }, compacto(p.receitaEmRisco)),
          h('span', { class: 'nota' }, `${p.alunosEmRiscoAlto} alunos com risco alto de evasão →`)),
        kpi('Inadimplência', compacto(k.valorEmAtraso), `${pct(k.inadimplenciaPercentual)} do MRR · ${k.inadimplentes} alunos`,
          k.inadimplenciaPercentual > 6 ? 'alerta' : ''),
        kpi('Cancelamentos (30 dias)', pct(k.churnPercentual), `${k.cancelamentos30Dias} cancelaram · ${k.novos30Dias} novos`),
        kpi('Na academia agora', numero(k.dentroAgora), `${ocupacao}% da capacidade · ${numero(k.visitasHoje)} entradas hoje`)),

      h('div', { class: 'grade-12' },
        h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Receita por mês'), h('p', {}, 'últimos 6 meses')),
          legenda([['s1', 'Faturado'], ['s3', 'Recebido']]), barrasPorMes(p.receita)),
        h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, unidade ? 'Unidade' : 'Unidades'), h('p', {}, 'receita e lotação agora')),
          h('div', { class: 'lista' }, p.unidades.filter((u) => !estado.unidadeId || String(u.id) === estado.unidadeId).map(linhaDeUnidade)))),

      h('div', { class: 'grade-12' },
        h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Quando a academia enche'), h('p', {}, 'entradas por dia e hora · 28 dias')),
          mapaDeCalor(p.mapaDeCalor)),
        h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Planos'), h('p', {}, 'alunos ativos')),
          barrasHorizontais(p.planos.map((x) => ({ rotulo: x.plano, valor: x.alunos, texto: `${x.alunos} · ${compacto(x.receita)}` })), { cor: 's1' }))),

      h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Quem agir primeiro'), h('a', { href: '#/risco' }, 'ver todos')),
        p.maisArriscados.length ? h('div', { class: 'lista' }, p.maisArriscados.map(linhaDeRisco))
          : h('p', { class: 'mudo' }, 'Nenhum aluno com sinais de risco.')));
  }

  function resumo(p, unidade, maisRisco) {
    const k = p.kpis;
    const partes = [`${numero(k.ativos)} alunos ativos geram ${reaisInteiros(k.receitaRecorrente)} por mês.`];
    if (p.alunosEmRiscoAlto) partes.push(`${reaisInteiros(p.receitaEmRisco)} desse total (${pct((p.receitaEmRisco / k.receitaRecorrente) * 100)}) está com alunos em risco alto de sair.`);
    if (!unidade && maisRisco && maisRisco.riscoAlto) partes.push(`A maior concentração é em ${maisRisco.nome.replace('Pulso ', '')}.`);
    return partes.join(' ');
  }

  raiz.append(esqueleto(5));
  try { await carregar(true); } catch (e) { limpar(raiz).append(erroNaTela(e)); }
  temporizador = setInterval(() => carregar(false).catch(() => {}), 15000);
  const sair = estado.aoMudarUnidade(() => carregar(false).catch(() => {}));
  return () => { vivo = false; clearInterval(temporizador); sair(); };
}

function kpi(rotulo, valor, nota, classe = '') {
  return h('div', { class: 'kpi ' + classe }, h('span', { class: 'rotulo' }, rotulo), h('span', { class: 'valor' }, valor), h('span', { class: 'nota' }, nota));
}

function legenda(itens) {
  return h('div', { class: 'legenda' }, itens.map(([cor, nome]) => h('span', {}, h('i', { style: `background:var(--${cor})` }), nome)));
}

function linhaDeUnidade(u) {
  const ocup = u.capacidade ? Math.min(100, Math.round((u.dentro / u.capacidade) * 100)) : 0;
  return h('div', { class: 'unidade-linha' },
    h('div', { class: 'cab' }, h('strong', {}, u.nome.replace('Pulso ', '')), h('span', { class: 'mudo' }, `${u.cidade}/${u.uf}`)),
    h('div', { class: 'medidor' + (ocup >= 90 ? ' cheio' : ''), role: 'img', 'aria-label': `${ocup}% da capacidade` }, h('i', { style: `width:${ocup}%` })),
    h('div', { class: 'meta num' }, h('span', {}, `${u.dentro}/${u.capacidade} agora`), h('span', {}, `${numero(u.ativos)} ativos`),
      h('span', {}, compacto(u.receita) + '/mês'), u.riscoAlto ? h('span', { class: 'selo critico' }, `${u.riscoAlto} em risco`) : null));
}

export function linhaDeRisco(a) {
  return h('a', { class: 'item risco-item', href: `#/alunos/${a.alunoId}` },
    h('span', { class: 'avatar' }, a.nome.split(' ').slice(0, 2).map((x) => x[0]).join('')),
    h('span', { class: 'corpo' }, h('strong', {}, a.nome), h('span', {}, `${a.unidade.replace('Pulso ', '')} · ${a.plano} · ${reais(a.valorMensal)}`),
      h('span', { class: 'fatores' }, a.fatores.map((f) => h('span', { class: 'selo' }, f)))),
    h('span', { class: 'fim' }, h('span', { class: 'risco ' + a.nivel }, icone(a.nivel === 'ALTO' ? 'alerta' : 'risco', 14), `${a.pontos} · ${rotuloNivel(a.nivel)}`)));
}

export const rotuloNivel = (n) => ({ ALTO: 'Alto', MEDIO: 'Médio', BAIXO: 'Baixo' }[n] || n);

// Risco de evasão: quem tende a cancelar, por quê, e quanto de receita está em jogo.
import { h, limpar, compacto, esqueleto, vazio, erroNaTela, numero } from './ui.js';
import { api, consulta } from './api.js';
import { estado } from './estado.js';
import { linhaDeRisco } from './painel.js';

export async function renderizar(raiz) {
  let nivel = 'ALTO';
  const lista = h('div', { class: 'cartao' }, esqueleto(5));
  const resumo = h('section', { class: 'kpis' });
  const abas = h('div', { class: 'dias', role: 'group', 'aria-label': 'Nível de risco' });

  async function carregar() {
    try {
      const todos = await api.get('/api/risco' + consulta({ unidadeId: estado.unidadeId }));
      const por = (n) => todos.filter((x) => x.nivel === n);
      const soma = (l) => l.reduce((s, x) => s + Number(x.valorMensal), 0);
      limpar(resumo).append(
        bloco('Risco alto', por('ALTO').length, `${compacto(soma(por('ALTO')))} por mês em jogo`, 'alerta'),
        bloco('Risco médio', por('MEDIO').length, `${compacto(soma(por('MEDIO')))} por mês`),
        bloco('Com algum sinal', todos.length, 'de toda a base ativa'));
      limpar(abas).append(...[['ALTO', 'Alto'], ['MEDIO', 'Médio'], ['BAIXO', 'Baixo']].map(([v, t]) =>
        h('button', { class: 'dia', type: 'button', 'aria-pressed': String(nivel === v), onclick: () => { nivel = v; carregar(); } }, h('small', {}, t), h('strong', {}, por(v).length))));
      const itens = por(nivel);
      limpar(lista).append(itens.length ? h('div', { class: 'lista' }, itens.slice(0, 60).map(linhaDeRisco)) : vazio('Ninguém neste nível'));
    } catch (e) { limpar(lista).append(erroNaTela(e)); }
  }
  const sair = estado.aoMudarUnidade(carregar);

  const exportar = h('a', { class: 'botao', href: '#', onclick: (e) => {
    e.preventDefault();
    location.href = '/api/risco.csv' + consulta({ unidadeId: estado.unidadeId, nivel });
  } }, 'Exportar lista (CSV)');
  raiz.append(h('div', { class: 'topo' }, h('div', {}, h('h1', {}, 'Risco de evasão'),
    h('p', {}, 'Nota de 0 a 100 por aluno: tempo sem treinar, queda de frequência, mensalidade vencida e início de jornada. Cada ponto tem um motivo explicado.')),
    h('div', { class: 'acoes' }, exportar)),
    resumo, abas, lista);
  await carregar();
  return sair;
}

function bloco(rotulo, valor, nota, classe = '') {
  return h('div', { class: 'kpi ' + classe }, h('span', { class: 'rotulo' }, rotulo), h('span', { class: 'valor' }, numero(valor)), h('span', { class: 'nota' }, nota));
}

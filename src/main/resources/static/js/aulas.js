// Agenda de aulas coletivas: vagas, fila de espera e reserva "como" um aluno escolhido.
import { h, icone, limpar, toast, esqueleto, vazio, hojeISO, somaDias, diaDaSemana, hora } from './ui.js';
import { api, consulta, referencias } from './api.js';
import { estado } from './estado.js';

export async function renderizar(raiz) {
  const { unidades } = await referencias();
  const hoje = hojeISO();
  let dia = hoje;
  let unidadeId = estado.unidadeId || String(unidades[0].id);
  let aluno = null; // { id, nome }

  const grade = h('div', { class: 'aulas' }, esqueleto(3));
  const diasEl = h('div', { class: 'dias', role: 'group', 'aria-label': 'Dia' });
  const sel = h('select', { class: 'entrada', 'aria-label': 'Unidade', style: 'width:auto' },
    unidades.map((u) => h('option', { value: u.id, selected: String(u.id) === unidadeId }, u.nome.replace('Pulso ', ''))));
  sel.addEventListener('change', () => { unidadeId = sel.value; carregar(); });

  // seletor de aluno: busca pelo nome e escolhe quem vai reservar
  const buscaAluno = h('input', { class: 'entrada', placeholder: 'Reservar como… (nome do aluno)', 'aria-label': 'Aluno', autocomplete: 'off' });
  const resultados = h('div', { class: 'sugestoes' });
  let espera;
  buscaAluno.addEventListener('input', () => {
    clearTimeout(espera);
    espera = setTimeout(async () => {
      if (buscaAluno.value.trim().length < 2) { limpar(resultados); return; }
      const r = await api.get(`/api/alunos?busca=${encodeURIComponent(buscaAluno.value)}&situacao=ATIVA&tamanho=6`);
      limpar(resultados).append(...r.conteudo.map((a) => h('button', { class: 'botao pequeno', type: 'button', onclick: () => escolher(a) }, `${a.nome} · ${a.plano}`)));
    }, 250);
  });
  const quem = h('div', { class: 'selo marca', hidden: true });
  function escolher(a) {
    aluno = { id: a.id, nome: a.nome, unidadeId: a.unidadeId };
    limpar(resultados); buscaAluno.value = '';
    quem.hidden = false; quem.textContent = `Reservando como ${a.nome}`;
    carregar();
  }

  function desenharDias() {
    limpar(diasEl).append(...Array.from({ length: 5 }, (_, i) => {
      const d = somaDias(hoje, i);
      return h('button', { class: 'dia', type: 'button', 'aria-pressed': String(d === dia), onclick: () => { dia = d; desenharDias(); carregar(); } },
        h('small', {}, i === 0 ? 'hoje' : diaDaSemana(d)), h('strong', {}, d.slice(8)));
    }));
  }

  async function carregar() {
    limpar(grade).append(esqueleto(3));
    try {
      const aulas = await api.get('/api/aulas' + consulta({ unidadeId, dia, alunoId: aluno?.id }));
      limpar(grade);
      if (!aulas.length) grade.append(vazio('Sem aulas neste dia'));
      aulas.forEach((a) => grade.append(cartao(a)));
    } catch (e) { limpar(grade).append(vazio('Erro ao carregar', e.message)); }
  }

  function cartao(a) {
    const livres = a.vagas - a.confirmadas;
    const pct = Math.round((a.confirmadas / a.vagas) * 100);
    const passou = dia === hoje && hora(a.inicio) < new Date().toTimeString().slice(0, 5);
    return h('article', { class: 'aula' + (a.reservada ? ' reservada' : '') },
      h('div', { class: 'topo-aula' }, h('span', { class: 'emoji' }, a.icone),
        h('div', {}, h('strong', {}, a.modalidade), h('div', { class: 'mudo', style: 'font-size:.82rem' }, `${a.instrutor} · ${a.duracaoMin} min`)),
        h('span', { class: 'horario' }, hora(a.inicio))),
      h('div', { class: 'medidor' + (livres <= 0 ? ' cheio' : ''), role: 'img', 'aria-label': `${a.confirmadas} de ${a.vagas} vagas ocupadas` }, h('i', { style: `width:${Math.min(100, pct)}%` })),
      h('div', { class: 'vagas num' }, h('span', {}, livres > 0 ? `${livres} vagas livres` : 'Lotada'), a.espera ? h('span', { class: 'selo atencao' }, `${a.espera} na fila`) : h('span', {}, `${a.confirmadas}/${a.vagas}`)),
      a.reservada ? h('span', { class: 'selo ok' }, icone('ok', 14), 'Você já reservou')
        : h('button', { class: 'botao ' + (livres > 0 ? 'primario' : ''), type: 'button', disabled: passou || !aluno,
          title: !aluno ? 'Escolha um aluno acima' : '', onclick: () => reservar(a) }, livres > 0 ? 'Reservar vaga' : 'Entrar na fila de espera'));
  }

  async function reservar(a) {
    try {
      const r = await api.post(`/api/aulas/${a.id}/reservas`, { alunoId: aluno.id });
      toast(r.status === 'CONFIRMADA' ? 'Vaga confirmada!' : `Aula cheia: você é o ${r.posicaoNaFila}º da fila de espera.`);
      carregar();
    } catch (e) { toast(e.message, 'erro'); }
  }

  raiz.append(
    h('div', { class: 'topo' }, h('div', {}, h('h1', {}, 'Aulas'), h('p', {}, 'Cheia? Entra na fila. Alguém cancelou? O primeiro da fila é promovido automaticamente.')), sel),
    h('div', { class: 'filtros' }, h('div', { class: 'busca' }, icone('busca', 18), buscaAluno), quem), resultados, diasEl, grade);
  desenharDias();
  await carregar();
}

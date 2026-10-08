// Catraca: simula a recepção (libera ou nega com o motivo) e mostra quem está dentro, ao vivo.
import { h, icone, limpar, toast, vazio, iniciais, esqueleto } from './ui.js';
import { api, referencias } from './api.js';
import { estado } from './estado.js';

export async function renderizar(raiz) {
  const { unidades } = await referencias();
  let unidadeId = estado.unidadeId || String(unidades[0].id);
  let vivo = true;

  const seletor = h('select', { class: 'entrada', 'aria-label': 'Unidade da catraca', style: 'width:auto' },
    unidades.map((u) => h('option', { value: u.id, selected: String(u.id) === unidadeId }, u.nome.replace('Pulso ', ''))));
  seletor.addEventListener('change', () => { unidadeId = seletor.value; atualizarAoVivo(); });

  const cpf = h('input', { class: 'entrada', placeholder: 'CPF do aluno', inputmode: 'numeric', autocomplete: 'off', 'aria-label': 'CPF do aluno' });
  const visor = h('div', { class: 'visor', 'aria-live': 'polite' }, icone('catraca', 44), h('strong', {}, 'Aguardando'), h('span', { class: 'suave' }, 'Digite o CPF ou escolha um aluno abaixo.'));
  const sugestoes = h('div', { class: 'sugestoes' });
  const aoVivo = h('div', { class: 'cartao' }, esqueleto(3));

  function mostrar(d, entrada) {
    const ok = d.liberado;
    limpar(visor).append(icone(ok ? 'ok' : 'alerta', 44), h('strong', {}, ok ? (entrada ? 'Liberado' : 'Saída registrada') : 'Acesso negado'), h('span', {}, d.motivo));
    visor.className = 'visor ' + (ok ? 'liberado' : 'negado');
  }

  async function passar(entrada) {
    const texto = cpf.value.trim();
    if (!texto) { cpf.focus(); return; }
    try {
      let d;
      if (entrada) d = await api.post('/api/catraca/entrada', { cpf: texto, unidadeId: Number(unidadeId) });
      else {
        const a = await api.get(`/api/alunos?busca=${encodeURIComponent(texto)}&tamanho=1`);
        if (!a.conteudo.length) throw new Error('Nenhum aluno com esse CPF.');
        d = await api.post('/api/catraca/saida', { alunoId: a.conteudo[0].id });
      }
      mostrar(d, entrada);
      if (d.liberado) cpf.value = '';
      atualizarAoVivo();
    } catch (e) { mostrar({ liberado: false, motivo: e.message }, entrada); }
  }

  async function atualizarAoVivo() {
    try {
      const r = await api.get(`/api/catraca/ao-vivo?unidadeId=${unidadeId}`);
      if (!vivo) return;
      const pct = Math.min(100, Math.round((r.ocupacao / r.capacidade) * 100));
      limpar(aoVivo).append(
        h('header', {}, h('h2', {}, 'Na unidade agora'), h('span', { class: 'ao-vivo' }, 'ao vivo')),
        h('div', { style: 'display:flex;align-items:baseline;gap:8px;margin-bottom:8px' }, h('strong', { class: 'num', style: 'font-size:2rem' }, r.ocupacao), h('span', { class: 'mudo' }, `de ${r.capacidade} (${pct}%)`)),
        h('div', { class: 'medidor' + (pct >= 90 ? ' cheio' : '') }, h('i', { style: `width:${pct}%` })),
        h('div', { class: 'presentes lista', style: 'margin-top:12px' }, r.presentes.length ? r.presentes.slice(0, 40).map((p) => h('div', { class: 'item' },
          h('span', { class: 'avatar' }, iniciais(p.nome)),
          h('a', { class: 'corpo', href: `#/alunos/${p.alunoId}`, style: 'color:inherit' }, h('strong', {}, p.nome), h('span', {}, `há ${p.minutos} min`)),
          h('button', { class: 'botao pequeno', onclick: async () => { try { await api.post('/api/catraca/saida', { alunoId: p.alunoId }); atualizarAoVivo(); } catch (e) { toast(e.message, 'erro'); } } }, 'Saiu')))
          : vazio('Ninguém por aqui agora')));
    } catch { /* tenta de novo no próximo ciclo */ }
  }

  async function sugerir() {
    try {
      const a = await api.get(`/api/alunos?situacao=ATIVA&unidadeId=${unidadeId}&tamanho=6`);
      limpar(sugestoes).append(...a.conteudo.map((x) => h('button', { class: 'botao pequeno', type: 'button', onclick: () => { cpf.value = x.cpf; cpf.focus(); } }, x.nome.split(' ').slice(0, 2).join(' '))));
    } catch { /* sugestões são opcionais */ }
  }
  seletor.addEventListener('change', sugerir);

  raiz.append(
    h('div', { class: 'topo' }, h('div', {}, h('h1', {}, 'Catraca'), h('p', {}, 'Libera ou nega a entrada pelas regras da matrícula: plano, unidade, trancamento, mensalidade vencida há mais de 10 dias e lotação.')), seletor),
    h('div', { class: 'catraca' },
      h('div', { class: 'pilha' }, h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, 'Recepção')),
        h('form', { class: 'pilha', onsubmit: (e) => { e.preventDefault(); passar(true); } }, cpf, visor,
          h('div', { style: 'display:flex;gap:8px' }, h('button', { class: 'botao primario', type: 'submit', style: 'flex:1' }, 'Entrar'),
            h('button', { class: 'botao', type: 'button', style: 'flex:1', onclick: () => passar(false) }, 'Sair'))),
        h('p', { class: 'mudo', style: 'margin-top:12px;font-size:.82rem' }, 'Alunos desta unidade (toque para preencher o CPF):'), sugestoes)),
      aoVivo));

  sugerir();
  await atualizarAoVivo();
  const t = setInterval(atualizarAoVivo, 5000);
  return () => { vivo = false; clearInterval(t); };
}

// "Por dentro do JPA": o chip mostra quantas consultas SQL a última chamada custou; a gaveta lista o SQL real.
import { h, icone, limpar } from './ui.js';
import { api, aoMedir, ultima } from './api.js';
import { estado } from './estado.js';

export function iniciarRastro() {
  if (!estado.eh('DIRETORIA', 'GERENTE')) return; // o SQL interno é só da gestão
  const chip = h('button', { class: 'chip-sql', type: 'button', 'aria-label': 'Abrir o SQL gerado pelas últimas chamadas' },
    icone('bolt', 16), h('span', {}, 'SQL ', h('b', { id: 'chip-n' }, '–')), h('span', { id: 'chip-ms' }, ''));
  const corpo = h('div', { class: 'corpo' });
  const gaveta = h('aside', { class: 'gaveta', 'aria-label': 'SQL gerado pelo Hibernate' },
    h('header', {}, h('div', {}, h('h2', {}, 'Por dentro do JPA'),
      h('p', { class: 'mudo' }, 'O SQL que o Hibernate gerou para cada chamada desta tela.')),
      h('button', { class: 'icone-botao', 'aria-label': 'Fechar', onclick: () => gaveta.classList.remove('aberta') }, icone('fechar'))),
    corpo);
  document.body.append(chip, gaveta);

  aoMedir((u) => {
    chip.querySelector('#chip-n').textContent = u.consultas;
    chip.querySelector('#chip-ms').textContent = `· ${u.ms} ms`;
    if (gaveta.classList.contains('aberta')) recarregar();
  });

  async function recarregar() {
    try {
      const itens = await api.get('/api/rastro/recentes?quantos=25');
      limpar(corpo);
      if (!itens.length) { corpo.append(h('p', { class: 'mudo' }, 'Nenhuma chamada ainda.')); return; }
      itens.forEach((r, i) => corpo.append(h('details', { class: 'req', open: i === 0 },
        h('summary', {}, h('span', { class: 'selo ' + (r.sql.length > 12 ? 'atencao' : 'ok') }, `${r.sql.length} SQL`),
          h('span', { class: 'caminho' }, `${r.metodo} ${r.caminho}`), h('span', { class: 'mudo num' }, `${r.milissegundos} ms`)),
        h('div', { class: 'sqls' }, r.sql.length ? r.sql.map((q) => h('div', { class: 'codigo' }, q)) : h('span', { class: 'mudo' }, 'Nenhuma consulta (nada tocou o banco).')))));
    } catch { /* gaveta é auxiliar */ }
  }

  chip.addEventListener('click', () => { gaveta.classList.add('aberta'); recarregar(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') gaveta.classList.remove('aberta'); });
  if (ultima.consultas != null) chip.querySelector('#chip-n').textContent = ultima.consultas;
}

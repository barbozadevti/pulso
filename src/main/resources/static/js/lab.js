// Laboratório de JPA: o N+1 medido em SQL real e a disputa pela última vaga com threads de verdade.
import { h, icone, limpar, toast, numero } from './ui.js';
import { api } from './api.js';
import { barrasHorizontais } from './charts.js';

const CONCEITOS = [
  ['@OneToMany / @ManyToOne', 'Aluno ↔ Matrícula ↔ Mensalidade; cascade e orphanRemoval nas avaliações físicas.'],
  ['@ManyToMany', 'Plano ↔ Modalidade com tabela de junção (plano_modalidade).'],
  ['@Embeddable', 'Endereço dentro da tabela do aluno, sem identidade própria.'],
  ['Herança SINGLE_TABLE', 'Pagamento (Pix, cartão, boleto) numa tabela só, discriminada por "forma".'],
  ['@MappedSuperclass + auditoria', 'criado_em e atualizado_em preenchidos pelo Spring Data, com o relógio da aplicação.'],
  ['@Version (bloqueio otimista)', 'Aula: reservas simultâneas não geram overbooking; quem perde refaz.'],
  ['Specification', 'Filtros opcionais e combináveis na lista de alunos, com subconsulta de situação.'],
  ['@EntityGraph e join fetch', 'Carregam as associações junto e eliminam o N+1.'],
  ['Projeção em DTO', 'SELECT new …: relatórios sem carregar entidades.'],
  ['JPQL agregado', 'Painel executivo: SUM, COUNT e GROUP BY no banco, não em memória.'],
  ['Flyway + ddl-auto=validate', 'O esquema é versionado em SQL e o Hibernate só confere se bate com as entidades.'],
  ['H2 e PostgreSQL', 'O mesmo SQL portável roda nos dois; troque pelo perfil "postgres".'],
];

export async function renderizar(raiz) {
  const comparacao = h('div', { class: 'grade-2' });
  const graficoComparacao = h('div');
  const disputa = h('div');

  const botaoMedir = h('button', { class: 'botao primario', type: 'button', onclick: medir }, icone('bolt', 18), 'Medir agora');
  async function medir() {
    botaoMedir.disabled = true;
    try {
      const r = await api.get('/api/laboratorio/n-mais-um');
      const pior = Math.max(...r.map((x) => x.consultas));
      limpar(comparacao).append(...r.map((x) => h('section', { class: 'cartao modo ' + (x.modo === 'INGENUO' ? 'ruim' : 'bom') },
        h('header', {}, h('h2', {}, x.titulo), h('span', { class: 'mudo num' }, `${x.milissegundos} ms`)),
        h('div', { class: 'placar' }, h('strong', {}, numero(x.consultas)), h('span', { class: 'suave' }, x.consultas === 1 ? 'consulta SQL' : 'consultas SQL')),
        h('p', { class: 'suave', style: 'font-size:.88rem' }, x.explicacao),
        h('details', {}, h('summary', { style: 'cursor:pointer;font-weight:700;font-size:.85rem' }, 'Ver o SQL (primeiras consultas)'),
          h('div', { class: 'pilha', style: 'margin-top:8px;gap:6px' }, x.sql.map((q) => h('div', { class: 'codigo' }, q)))))));
      limpar(graficoComparacao).append(h('section', { class: 'cartao', style: 'margin-top:16px' },
        h('header', {}, h('h2', {}, 'Mesma tela, quatro custos'), h('p', {}, '50 matrículas com aluno, unidade e plano')),
        barrasHorizontais(r.map((x) => ({ rotulo: x.titulo.replace(' (lazy padrão)', ''), valor: x.consultas, texto: `${x.consultas} consultas`, cor: x.modo === 'INGENUO' ? 's2' : 's3' })),
          { formato: (v) => `${v} consultas` }),
        h('p', { class: 'suave', style: 'margin-top:12px' }, `A abordagem ingênua faz ${Math.round(pior / Math.max(1, Math.min(...r.map((x) => x.consultas))))}× mais idas ao banco para mostrar exatamente a mesma coisa.`)));
    } catch (e) { toast(e.message, 'erro'); } finally { botaoMedir.disabled = false; }
  }

  // ---- disputa pela vaga
  const nConc = h('input', { type: 'range', min: 10, max: 40, value: 30, 'aria-label': 'Concorrentes' });
  const nVagas = h('input', { type: 'range', min: 1, max: 6, value: 3, 'aria-label': 'Vagas' });
  const rConc = h('b', {}, '30'), rVagas = h('b', {}, '3');
  nConc.addEventListener('input', () => { rConc.textContent = nConc.value; });
  nVagas.addEventListener('input', () => { rVagas.textContent = nVagas.value; });
  const botaoDisputa = h('button', { class: 'botao destaque', type: 'button', onclick: disputar }, icone('aulas', 18), 'Disparar a disputa');
  async function disputar() {
    botaoDisputa.disabled = true;
    limpar(disputa).append(h('p', { class: 'suave pulsando' }, 'Threads reservando ao mesmo tempo…'));
    try {
      const d = await api.post(`/api/laboratorio/disputa?concorrentes=${nConc.value}&vagas=${nVagas.value}`);
      const quadrados = [...Array(d.confirmadas).fill('conf'), ...Array(d.naEspera).fill('espera')];
      limpar(disputa).append(
        h('div', { class: 'corrida', 'aria-label': `${d.confirmadas} confirmadas e ${d.naEspera} na fila` }, quadrados.map((c) => h('i', { class: c }))),
        h('div', { class: 'resultado-linha' },
          resultado('Vagas', d.vagas), resultado('Confirmadas', d.confirmadas, 'ok'), resultado('Na fila', d.naEspera),
          resultado('Conflitos resolvidos', d.conflitosResolvidos), resultado('Tempo', `${d.milissegundos} ms`),
          resultado('Overbooking', d.overbooking ? 'SIM' : 'Não', d.overbooking ? 'critico' : 'ok')));
    } catch (e) { limpar(disputa); toast(e.message, 'erro'); } finally { botaoDisputa.disabled = false; }
  }

  raiz.append(
    h('div', { class: 'topo' }, h('div', {}, h('h1', {}, 'Laboratório de JPA'),
      h('p', {}, 'Duas demonstrações ao vivo do que o Spring Data JPA faz por baixo. O contador “SQL” no canto da tela mostra o custo de cada chamada.')),
      h('div', { class: 'acoes' }, h('a', { class: 'botao', href: '/swagger-ui.html', target: '_blank', rel: 'noopener' }, 'API (Swagger)'))),
    h('section', { class: 'cartao' }, h('header', {}, h('h2', {}, '1 · O problema N+1'), botaoMedir),
      h('p', { class: 'suave' }, 'Listar matrículas e mostrar o nome do aluno, da unidade e do plano parece uma tarefa só. Com carregamento lazy, vira dezenas de consultas. Rode e compare as quatro formas de resolver.')),
    h('div', { style: 'height:16px' }), comparacao, graficoComparacao,
    h('section', { class: 'cartao', style: 'margin-top:16px' }, h('header', {}, h('h2', {}, '2 · Disputa pela última vaga'), botaoDisputa),
      h('p', { class: 'suave' }, 'Dezenas de threads tentam reservar a mesma aula no mesmo instante. Sem controle, todas passariam e a aula estouraria. Com @Version, só as vagas existentes são confirmadas e o resto vai para a fila.'),
      h('div', { class: 'formulario', style: 'margin:14px 0' },
        h('label', { class: 'campo' }, h('span', {}, 'Concorrentes: ', rConc), nConc), h('label', { class: 'campo' }, h('span', {}, 'Vagas na aula: ', rVagas), nVagas)),
      disputa),
    h('section', { class: 'cartao', style: 'margin-top:16px' }, h('header', {}, h('h2', {}, 'Conceitos de JPA usados neste projeto')),
      h('div', { class: 'grade-2' }, CONCEITOS.map(([t, d]) => h('div', { class: 'item', style: 'align-items:flex-start' },
        h('span', { class: 'corpo' }, h('strong', { style: 'white-space:normal' }, t), h('span', {}, d)))))));
}

function resultado(rotulo, valor, tom = '') {
  return h('div', { class: 'kpi' }, h('span', { class: 'rotulo' }, rotulo), h('span', { class: 'valor ' + tom, style: tom === 'ok' ? 'color:var(--ok)' : tom === 'critico' ? 'color:var(--critico)' : '' }, String(valor)));
}

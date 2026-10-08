// Lista de alunos: busca, filtros combináveis (Specification no servidor) e paginação.
import { h, icone, limpar, esqueleto, vazio, erroNaTela, iniciais, modal, toast, numero } from './ui.js';
import { api, consulta, referencias } from './api.js';
import { estado } from './estado.js';

const SITUACOES = [['', 'Todas as situações'], ['ATIVA', 'Ativos'], ['TRANCADA', 'Trancados'], ['CANCELADA', 'Sem matrícula']];

export async function renderizar(raiz) {
  const filtro = { busca: '', situacao: '', pagina: 0 };
  const lista = h('div', { class: 'cartao' }, esqueleto(5));
  const rodape = h('div', { class: 'paginacao' });
  let espera;

  const busca = h('input', { class: 'entrada', type: 'search', placeholder: 'Nome ou CPF', 'aria-label': 'Buscar aluno', autocomplete: 'off' });
  busca.addEventListener('input', () => { clearTimeout(espera); espera = setTimeout(() => { filtro.busca = busca.value; filtro.pagina = 0; carregar(); }, 250); });
  const situacao = h('select', { class: 'entrada', 'aria-label': 'Situação' }, SITUACOES.map(([v, t]) => h('option', { value: v }, t)));
  situacao.addEventListener('change', () => { filtro.situacao = situacao.value; filtro.pagina = 0; carregar(); });

  raiz.append(
    h('div', { class: 'topo' }, h('div', {}, h('h1', {}, 'Alunos'), h('p', {}, 'Busca por nome ou CPF, filtro por unidade e situação da matrícula.')),
      h('div', { class: 'acoes' }, h('button', { class: 'botao primario', type: 'button', onclick: novoAluno }, icone('mais', 18), 'Novo aluno'))),
    h('div', { class: 'filtros' }, h('div', { class: 'busca' }, icone('busca', 18), busca), situacao),
    lista, rodape);

  async function carregar() {
    limpar(lista).append(esqueleto(5));
    try {
      const r = await api.get('/api/alunos' + consulta({ busca: filtro.busca, situacao: filtro.situacao, unidadeId: estado.unidadeId, pagina: filtro.pagina, tamanho: 12 }));
      limpar(lista);
      if (!r.conteudo.length) lista.append(vazio('Nenhum aluno encontrado', 'Tente outro nome, CPF ou filtro.'));
      else lista.append(h('div', { class: 'lista' }, r.conteudo.map(linha)));
      limpar(rodape).append(h('span', { class: 'num' }, `${numero(r.total)} alunos · página ${r.pagina + 1} de ${Math.max(1, r.paginas)}`),
        h('span', { class: 'acoes', style: 'display:flex;gap:8px' },
          h('button', { class: 'botao pequeno', disabled: r.pagina === 0, onclick: () => { filtro.pagina--; carregar(); } }, 'Anterior'),
          h('button', { class: 'botao pequeno', disabled: r.pagina + 1 >= r.paginas, onclick: () => { filtro.pagina++; carregar(); } }, 'Próxima')));
    } catch (e) { limpar(lista).append(erroNaTela(e)); }
  }
  const sair = estado.aoMudarUnidade(() => { filtro.pagina = 0; carregar(); });
  await carregar();
  return sair;
}

const SELO = { ATIVA: ['ok', 'Ativo'], TRANCADA: ['atencao', 'Trancado'], CANCELADA: ['', 'Sem matrícula'] };
function linha(a) {
  const [classe, texto] = SELO[a.situacao] || ['', a.situacao];
  return h('a', { class: 'item', href: `#/alunos/${a.id}` },
    h('span', { class: 'avatar' }, iniciais(a.nome)),
    h('span', { class: 'corpo' }, h('strong', {}, a.nome), h('span', {}, `${a.plano || 'Sem plano'} · ${a.bairro}, ${a.cidade}/${a.uf} · ${a.idade} anos`)),
    h('span', { class: 'fim' }, h('span', { class: 'selo ' + classe }, texto), h('div', { class: 'mudo', style: 'font-size:.75rem;margin-top:4px' }, a.unidade.replace('Pulso ', ''))));
}

async function novoAluno() {
  const { unidades } = await referencias();
  const campo = (rotulo, nome, atrib = {}, largo = false) =>
    h('label', { class: 'campo' + (largo ? ' largo' : '') }, h('span', {}, rotulo), h('input', { class: 'entrada', name: nome, required: true, ...atrib }));
  const form = h('form', { class: 'formulario', novalidate: true },
    campo('Nome completo', 'nome', { autocomplete: 'name' }, true),
    campo('CPF', 'cpf', { inputmode: 'numeric', placeholder: '000.000.000-00' }),
    campo('Nascimento', 'nascimento', { type: 'date' }),
    campo('E-mail', 'email', { type: 'email', autocomplete: 'email' }, true),
    campo('Bairro', 'bairro'), campo('Cidade', 'cidade'),
    campo('UF', 'uf', { maxlength: 2, placeholder: 'ES' }),
    h('label', { class: 'campo' }, h('span', {}, 'Unidade'), h('select', { class: 'entrada', name: 'unidadeId' },
      unidades.map((u) => h('option', { value: u.id, selected: String(u.id) === estado.unidadeId }, u.nome.replace('Pulso ', ''))))));
  const erro = h('p', { role: 'alert', style: 'color:var(--critico);min-height:1.2em' });
  const salvar = h('button', { class: 'botao primario', type: 'submit' }, 'Cadastrar');
  const m = modal('Novo aluno', h('div', { class: 'pilha' }, form, erro,
    h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', type: 'button', onclick: () => m.fechar() }, 'Cancelar'), salvar)));
  salvar.addEventListener('click', () => form.requestSubmit());
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(form));
    d.unidadeId = Number(d.unidadeId);
    salvar.disabled = true;
    try {
      const ficha = await api.post('/api/alunos', d);
      m.fechar();
      toast('Aluno cadastrado.');
      location.hash = `#/alunos/${ficha.aluno.id}`;
    } catch (ex) { erro.textContent = ex.message; salvar.disabled = false; }
  });
}

// Casca do aplicativo: login, navegação por perfil, tema, "abrir no celular" e roteamento por hash.
import { h, icone, limpar, modal, iniciarDicas, erroNaTela, esqueleto, iniciais } from './ui.js';
import { api, referencias } from './api.js';
import { estado } from './estado.js';
import { iniciarRastro } from './rastro.js';
import { mostrarLogin } from './login.js';

const GESTAO = ['DIRETORIA', 'GERENTE'];
const EQUIPE = ['DIRETORIA', 'GERENTE', 'RECEPCAO'];
const TODOS = ['DIRETORIA', 'GERENTE', 'RECEPCAO', 'ALUNO'];

// Cada rota diz quem pode abri-la; o servidor confere de novo em toda chamada (aqui é só para o menu).
const ROTAS = [
  { caminho: '/painel', rotulo: 'Painel', curto: 'Painel', icone: 'painel', perfis: GESTAO, modulo: () => import('./painel.js') },
  { caminho: '/meu', rotulo: 'Meu espaço', curto: 'Eu', icone: 'alunos', perfis: ['ALUNO'] },
  { caminho: '/alunos', rotulo: 'Alunos', curto: 'Alunos', icone: 'alunos', perfis: EQUIPE, modulo: () => import('./alunos.js') },
  { caminho: '/catraca', rotulo: 'Catraca', curto: 'Catraca', icone: 'catraca', perfis: EQUIPE, modulo: () => import('./catraca.js') },
  { caminho: '/aulas', rotulo: 'Aulas', curto: 'Aulas', icone: 'aulas', perfis: TODOS, modulo: () => import('./aulas.js') },
  { caminho: '/risco', rotulo: 'Risco de evasão', curto: 'Risco', icone: 'risco', perfis: GESTAO, modulo: () => import('./risco.js') },
  { caminho: '/lab', rotulo: 'Laboratório JPA', curto: 'JPA', icone: 'lab', perfis: GESTAO, modulo: () => import('./lab.js') },
];
const PADRAO = { DIRETORIA: '/painel', GERENTE: '/painel', RECEPCAO: '/catraca', ALUNO: '/meu' };

const raiz = document.getElementById('conteudo');
const app = document.querySelector('.app');
const tabbar = document.getElementById('tabbar');
let limpeza = null;
let rodada = 0;
let unidades = [];

const rotasDoPerfil = () => ROTAS.filter((r) => r.perfis.includes(estado.sessao.perfil));

function logo() {
  const ns = 'http://www.w3.org/2000/svg';
  const s = document.createElementNS(ns, 'svg');
  s.setAttribute('viewBox', '0 0 64 64');
  const p = document.createElementNS(ns, 'path');
  p.setAttribute('d', 'M6 34h14l6-16 8 28 6-20 3 8h15');
  p.setAttribute('fill', 'none'); p.setAttribute('stroke', '#b6f23b'); p.setAttribute('stroke-width', '6');
  p.setAttribute('stroke-linecap', 'round'); p.setAttribute('stroke-linejoin', 'round');
  s.append(p);
  return h('span', { class: 'logo' }, s);
}

/** Diretoria escolhe a unidade; quem é de uma unidade só vê o nome dela; o aluno não precisa disso. */
function seletorDeUnidade(id) {
  if (estado.eh('ALUNO')) return null;
  if (estado.sessao.unidadeId) {
    const u = unidades.find((x) => x.id === estado.sessao.unidadeId);
    return h('div', { class: 'unidade-fixa' }, h('small', {}, 'Unidade'), h('strong', {}, u ? u.nome.replace('Pulso ', '') : '—'));
  }
  const sel = h('select', { class: 'entrada', id, 'aria-label': 'Unidade' },
    h('option', { value: '' }, 'Rede inteira'),
    ...unidades.map((u) => h('option', { value: u.id, selected: String(u.id) === estado.unidadeId }, u.nome.replace('Pulso ', ''))));
  sel.addEventListener('change', () => estado.definirUnidade(sel.value));
  return sel;
}

function caixaDoUsuario() {
  const s = estado.sessao;
  return h('div', { class: 'usuario-caixa' }, h('span', { class: 'avatar' }, iniciais(s.nome)),
    h('div', {}, h('strong', {}, s.nome), h('span', {}, s.rotuloDoPerfil)),
    h('button', { class: 'icone-botao', type: 'button', 'aria-label': 'Sair', title: 'Sair', onclick: sair }, icone('fechar', 18)));
}

async function sair() {
  try { await api.post('/api/auth/sair'); } catch { /* sai de qualquer jeito */ }
  location.hash = '';
  location.reload();
}

function montarCasca() {
  const rotas = rotasDoPerfil();
  const href = (r) => (r.caminho === '/meu' ? `#/alunos/${estado.sessao.alunoId}` : '#' + r.caminho);
  const lateral = document.getElementById('lateral');
  limpar(lateral).append(
    h('a', { class: 'marca', href: '#' + PADRAO[estado.sessao.perfil] }, logo(), h('span', {}, 'Pulso', h('small', {}, 'Gestão de academias'))),
    h('nav', { class: 'nav' }, rotas.map((r) => h('a', { href: href(r), dataset: { rota: r.caminho } }, icone(r.icone), r.rotulo))),
    h('div', { class: 'espaco' }),
    h('div', { class: 'rodape' },
      seletorDeUnidade('unidade-lateral'),
      estado.eh(...EQUIPE) ? h('button', { class: 'botao pequeno', type: 'button', onclick: abrirNoCelular }, icone('celular', 16), 'Abrir no celular') : null,
      h('button', { class: 'botao pequeno', type: 'button', onclick: () => estado.alternarTema() }, icone('lua', 16), 'Alternar tema'),
      caixaDoUsuario(),
      h('span', {}, 'Rede fictícia · dados de demonstração')));
  limpar(tabbar).append(...rotas.map((r) => h('a', { href: href(r), dataset: { rota: r.caminho } }, icone(r.icone, 22), r.curto)));
  tabbar.style.setProperty('grid-template-columns', `repeat(${rotas.length}, 1fr)`);
}

/** Faixa que só aparece no celular: marca, unidade, tema e sair (a lateral some em telas pequenas). */
function faixaMobile() {
  return h('div', { class: 'faixa-mobile' },
    h('a', { class: 'marca', href: '#' + PADRAO[estado.sessao.perfil] }, logo(), h('span', {}, 'Pulso')),
    h('div', { class: 'acoes' }, seletorDeUnidade('unidade-mobile'),
      h('button', { class: 'icone-botao', type: 'button', 'aria-label': 'Alternar tema', onclick: () => estado.alternarTema() }, icone('lua')),
      h('button', { class: 'icone-botao', type: 'button', 'aria-label': 'Sair', onclick: sair }, icone('fechar'))));
}

async function abrirNoCelular() {
  const e = await api.get('/api/rede');
  const corpo = h('div', { class: 'cabecalho-qr' },
    h('img', { class: 'qr', src: '/api/rede/qr.svg?' + Date.now(), alt: 'QR Code para abrir o Pulso no celular' }),
    h('div', {}, h('p', {}, 'Aponte a câmera do celular para o código. O celular precisa estar no mesmo Wi-Fi deste computador.'),
      h('p', { class: 'mudo' }, h('b', {}, e.url)),
      h('p', { class: 'mudo' }, 'Depois, no menu do navegador, escolha “Adicionar à tela inicial” para instalar como aplicativo.')));
  modal('Abrir no celular', corpo);
}

function marcarAtiva(caminho) {
  document.querySelectorAll('[data-rota]').forEach((a) => {
    const ativa = caminho === a.dataset.rota || caminho.startsWith(a.dataset.rota + '/');
    if (ativa) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
  });
}

async function rotear() {
  const s = estado.sessao;
  const caminho = (location.hash || '#' + PADRAO[s.perfil]).slice(1) || PADRAO[s.perfil];
  const ficha = caminho.match(/^\/alunos\/(\d+)$/);

  // atalho "Meu espaço" do aluno e proteção de rotas
  if (caminho === '/meu') { location.replace(`#/alunos/${s.alunoId}`); return; }
  if (ficha && s.perfil === 'ALUNO' && Number(ficha[1]) !== s.alunoId) { location.replace(`#/alunos/${s.alunoId}`); return; }
  const rota = ficha ? ROTAS.find((r) => r.caminho === '/alunos') : ROTAS.find((r) => r.caminho === caminho);
  if (!rota || (!ficha && !rota.perfis.includes(s.perfil))) { location.replace('#' + PADRAO[s.perfil]); return; }
  if (ficha && s.perfil !== 'ALUNO' && !EQUIPE.includes(s.perfil)) { location.replace('#' + PADRAO[s.perfil]); return; }

  marcarAtiva(s.perfil === 'ALUNO' && ficha ? '/meu' : ficha ? '/alunos' : rota.caminho);
  const eu = ++rodada;
  if (typeof limpeza === 'function') { try { limpeza(); } catch { /* ignora */ } }
  limpeza = null;
  limpar(raiz).append(faixaMobile(), esqueleto(4));
  try {
    const modulo = ficha ? await import('./aluno.js') : await rota.modulo();
    if (eu !== rodada) return;
    const conteudo = h('div', { class: 'pagina' });
    const retorno = await modulo.renderizar(conteudo, ficha ? { id: ficha[1] } : {});
    if (eu !== rodada) { if (typeof retorno === 'function') retorno(); return; }
    limpeza = retorno;
    limpar(raiz).append(faixaMobile(), conteudo);
    window.scrollTo({ top: 0 });
  } catch (e) {
    limpar(raiz).append(faixaMobile(), erroNaTela(e));
  }
}

function entrou(sessao) {
  estado.sessao = sessao;
  // quem é de uma unidade só trabalha nela; o aluno não usa o filtro de unidade
  estado.definirUnidade(sessao.unidadeId ? String(sessao.unidadeId) : '');
  app.hidden = false;
  tabbar.hidden = false;
  document.getElementById('login-raiz')?.remove();
  referencias().then((r) => { unidades = r.unidades; }).catch(() => { unidades = []; }).then(() => {
    montarCasca();
    iniciarRastro();
    addEventListener('hashchange', rotear);
    estado.aoMudarUnidade((id) => {
      document.querySelectorAll('#unidade-lateral, #unidade-mobile').forEach((x) => { x.value = id; });
    });
    if (!location.hash) location.replace('#' + PADRAO[sessao.perfil]);
    rotear();
  });
}

async function iniciar() {
  estado.aplicarTema();
  iniciarDicas();
  window.addEventListener('sessao-expirada', () => location.reload());
  app.hidden = true;
  tabbar.hidden = true;
  try {
    await api.get('/api/auth/csrf');
    entrou(await api.get('/api/auth/eu'));
  } catch (e) {
    if (e.status && e.status !== 401) { document.body.append(erroNaTela(e)); return; }
    const alvo = h('div', { id: 'login-raiz' });
    document.body.append(alvo);
    await mostrarLogin(alvo, (sessao) => { alvo.remove(); entrou(sessao); });
  }
}

iniciar();

if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
  navigator.serviceWorker.register('/sw.js').catch(() => { /* instalável é bônus */ });
}

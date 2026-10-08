// Casca do aplicativo: navegação, filtro de unidade, tema, "abrir no celular" e roteamento por hash.
import { h, icone, limpar, modal, iniciarDicas, erroNaTela, esqueleto } from './ui.js';
import { api, referencias } from './api.js';
import { estado } from './estado.js';
import { iniciarRastro } from './rastro.js';

const ROTAS = [
  { caminho: '/painel', rotulo: 'Painel', curto: 'Painel', icone: 'painel', modulo: () => import('./painel.js') },
  { caminho: '/alunos', rotulo: 'Alunos', curto: 'Alunos', icone: 'alunos', modulo: () => import('./alunos.js') },
  { caminho: '/catraca', rotulo: 'Catraca', curto: 'Catraca', icone: 'catraca', modulo: () => import('./catraca.js') },
  { caminho: '/aulas', rotulo: 'Aulas', curto: 'Aulas', icone: 'aulas', modulo: () => import('./aulas.js') },
  { caminho: '/risco', rotulo: 'Risco de evasão', curto: 'Risco', icone: 'risco', modulo: () => import('./risco.js') },
  { caminho: '/lab', rotulo: 'Laboratório JPA', curto: 'JPA', icone: 'lab', modulo: () => import('./lab.js') },
];

const raiz = document.getElementById('conteudo');
let limpeza = null;
let fichaAtual = 0;

function logo() {
  return h('span', { class: 'logo' }, svgLogo());
}
function svgLogo() {
  const ns = 'http://www.w3.org/2000/svg';
  const s = document.createElementNS(ns, 'svg');
  s.setAttribute('viewBox', '0 0 64 64');
  const p = document.createElementNS(ns, 'path');
  p.setAttribute('d', 'M6 34h14l6-16 8 28 6-20 3 8h15');
  p.setAttribute('fill', 'none'); p.setAttribute('stroke', '#b6f23b'); p.setAttribute('stroke-width', '6');
  p.setAttribute('stroke-linecap', 'round'); p.setAttribute('stroke-linejoin', 'round');
  s.append(p);
  return s;
}

function seletorDeUnidade(unidades, id) {
  const sel = h('select', { class: 'entrada', id, 'aria-label': 'Unidade' },
    h('option', { value: '' }, 'Rede inteira'),
    ...unidades.map((u) => h('option', { value: u.id, selected: String(u.id) === estado.unidadeId }, u.nome.replace('Pulso ', ''))));
  sel.addEventListener('change', () => estado.definirUnidade(sel.value));
  return sel;
}

async function montarCasca() {
  const { unidades } = await referencias();
  const lateral = document.getElementById('lateral');
  limpar(lateral).append(
    h('a', { class: 'marca', href: '#/painel' }, logo(), h('span', {}, 'Pulso', h('small', {}, 'Gestão de academias'))),
    h('nav', { class: 'nav' }, ROTAS.map((r) => h('a', { href: '#' + r.caminho, dataset: { rota: r.caminho } }, icone(r.icone), r.rotulo))),
    h('div', { class: 'espaco' }),
    h('div', { class: 'rodape' },
      h('label', { class: 'campo' }, h('span', {}, 'Unidade'), seletorDeUnidade(unidades, 'unidade-lateral')),
      h('button', { class: 'botao pequeno', type: 'button', onclick: abrirNoCelular }, icone('celular', 16), 'Abrir no celular'),
      h('button', { class: 'botao pequeno', type: 'button', onclick: () => estado.alternarTema() }, icone('lua', 16), 'Alternar tema'),
      h('span', {}, 'Rede fictícia · dados de demonstração')));
  limpar(document.getElementById('tabbar')).append(...ROTAS.map((r) =>
    h('a', { href: '#' + r.caminho, dataset: { rota: r.caminho } }, icone(r.icone, 22), r.curto)));
}

/** Faixa que só aparece no celular: marca, unidade, tema e QR (a lateral some em telas pequenas). */
async function faixaMobile() {
  const { unidades } = await referencias();
  return h('div', { class: 'faixa-mobile' },
    h('a', { class: 'marca', href: '#/painel' }, logo(), h('span', {}, 'Pulso')),
    h('div', { class: 'acoes' }, seletorDeUnidade(unidades, 'unidade-mobile'),
      h('button', { class: 'icone-botao', type: 'button', 'aria-label': 'Alternar tema', onclick: () => estado.alternarTema() }, icone('lua'))));
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
  const caminho = (location.hash || '#/painel').slice(1) || '/painel';
  const eficha = caminho.match(/^\/alunos\/(\d+)$/);
  const rota = eficha ? ROTAS[1] : ROTAS.find((r) => r.caminho === caminho) || ROTAS[0];
  marcarAtiva(eficha ? '/alunos' : rota.caminho);
  const eu = ++fichaAtual;
  if (typeof limpeza === 'function') { try { limpeza(); } catch { /* ignora */ } }
  limpeza = null;
  limpar(raiz).append(await faixaMobile(), esqueleto(4));
  try {
    const modulo = eficha ? await import('./aluno.js') : await rota.modulo();
    if (eu !== fichaAtual) return;
    const conteudo = h('div', { class: 'pagina' });
    const retorno = await modulo.renderizar(conteudo, eficha ? { id: eficha[1] } : {});
    if (eu !== fichaAtual) { if (typeof retorno === 'function') retorno(); return; }
    limpeza = retorno;
    limpar(raiz).append(await faixaMobile(), conteudo);
    window.scrollTo({ top: 0 });
  } catch (e) {
    limpar(raiz).append(erroNaTela(e));
  }
}

estado.aplicarTema();
iniciarDicas();
iniciarRastro();
montarCasca().then(() => {
  addEventListener('hashchange', rotear);
  rotear();
  // O seletor de unidade da casca e o do celular andam juntos; as telas escutam a mudança.
  estado.aoMudarUnidade((id) => {
    document.querySelectorAll('#unidade-lateral, #unidade-mobile').forEach((s) => { s.value = id; });
  });
});

if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
  navigator.serviceWorker.register('/sw.js').catch(() => { /* instalável é bônus */ });
}

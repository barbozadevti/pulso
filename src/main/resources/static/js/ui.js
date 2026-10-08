// Utilidades de interface: construtor de elementos, formatação, ícones, toast, modal e dica flutuante.

export function h(tag, atributos, ...filhos) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(atributos || {})) {
    if (v === false || v == null) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (k === 'estilo') for (const [p, val] of Object.entries(v)) el.style.setProperty(p, val);
    else if (v === true) el.setAttribute(k, '');
    else el.setAttribute(k, v);
  }
  anexar(el, filhos);
  return el;
}

export function anexar(el, filhos) {
  for (const f of filhos.flat(Infinity)) {
    if (f == null || f === false) continue;
    el.append(f instanceof Node ? f : document.createTextNode(String(f)));
  }
  return el;
}

const NS = 'http://www.w3.org/2000/svg';
export function svg(tag, atributos, ...filhos) {
  const el = document.createElementNS(NS, tag);
  for (const [k, v] of Object.entries(atributos || {})) {
    if (v == null || v === false) continue;
    if (k === 'class') el.setAttribute('class', v);
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v);
  }
  anexar(el, filhos);
  return el;
}

export function limpar(el) {
  el.replaceChildren();
  return el;
}

// ---- formatação (pt-BR)
const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const brlInteiro = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
const num = new Intl.NumberFormat('pt-BR');
export const reais = (v) => brl.format(Number(v) || 0);
export const reaisInteiros = (v) => brlInteiro.format(Number(v) || 0);
export const numero = (v) => num.format(Number(v) || 0);
export const compacto = (v) => {
  v = Number(v) || 0;
  if (Math.abs(v) >= 1e6) return 'R$ ' + (v / 1e6).toFixed(1).replace('.', ',') + ' mi';
  if (Math.abs(v) >= 1e3) return 'R$ ' + (v / 1e3).toFixed(1).replace('.', ',') + ' mil';
  return reais(v);
};
export const pct = (v) => (Number(v) || 0).toFixed(1).replace('.', ',') + '%';

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
export function data(iso) {
  if (!iso) return '—';
  const [a, m, d] = String(iso).slice(0, 10).split('-').map(Number);
  return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${a}`;
}
export function dataCurta(iso) {
  const [, m, d] = String(iso).slice(0, 10).split('-').map(Number);
  return `${d} ${MESES[m - 1]}`;
}
export const mesCurto = (aaaamm) => MESES[Number(aaaamm.slice(5, 7)) - 1];
export const hora = (iso) => String(iso).slice(11, 16);
export function dataHora(iso) {
  return iso ? `${data(iso)} ${hora(iso)}` : '—';
}
export const iniciais = (nome) => nome.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
export const hojeISO = () => new Date().toLocaleDateString('sv-SE');
export function somaDias(iso, dias) {
  const d = new Date(iso + 'T12:00:00');
  d.setDate(d.getDate() + dias);
  return d.toLocaleDateString('sv-SE');
}
export function diaDaSemana(iso) {
  return ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'][new Date(iso + 'T12:00:00').getDay()];
}

// ---- ícones (traço de 2px, 24x24)
const ICONES = {
  painel: ['M3 3v18h18', 'M7 15l4-5 3 3 5-7'],
  alunos: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M22 21v-2a4 4 0 0 0-3-3.87', 'M16 3.13a4 4 0 0 1 0 7.75'],
  catraca: ['M3 7V5a2 2 0 0 1 2-2h2', 'M17 3h2a2 2 0 0 1 2 2v2', 'M21 17v2a2 2 0 0 1-2 2h-2', 'M7 21H5a2 2 0 0 1-2-2v-2', 'M7 12h10'],
  aulas: ['M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'M16 2v4', 'M8 2v4', 'M3 10h18'],
  risco: ['M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z', 'M12 9v4', 'M12 17h.01'],
  lab: ['M10 2v7.5L4.5 19a2 2 0 0 0 1.8 3h11.4a2 2 0 0 0 1.8-3L14 9.5V2', 'M8.5 2h7', 'M7 16h10'],
  bolt: ['M13 2L3 14h9l-1 8 10-12h-9l1-8z'],
  lua: ['M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z'],
  sol: ['M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10z', 'M12 1v2', 'M12 21v2', 'M4.2 4.2l1.4 1.4', 'M18.4 18.4l1.4 1.4', 'M1 12h2', 'M21 12h2', 'M4.2 19.8l1.4-1.4', 'M18.4 5.6l1.4-1.4'],
  celular: ['M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z', 'M12 18h.01'],
  fechar: ['M18 6L6 18', 'M6 6l12 12'],
  busca: ['M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z', 'M21 21l-4.3-4.3'],
  mais: ['M12 5v14', 'M5 12h14'],
  voltar: ['M19 12H5', 'M12 19l-7-7 7-7'],
  ok: ['M20 6L9 17l-5-5'],
  alerta: ['M12 8v4', 'M12 16h.01', 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z'],
  seta: ['M5 12h14', 'M12 5l7 7-7 7'],
  relogio: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M12 6v6l4 2'],
};

export function icone(nome, tamanho = 20) {
  return svg('svg', { viewBox: '0 0 24 24', width: tamanho, height: tamanho, fill: 'none', stroke: 'currentColor',
    'stroke-width': 2, 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true', class: 'ico' },
    ...(ICONES[nome] || []).map((d) => svg('path', { d })));
}

// ---- toast
let temporizador;
export function toast(mensagem, tipo = 'info') {
  let el = document.getElementById('toast');
  if (!el) {
    el = h('div', { id: 'toast', role: 'status', 'aria-live': 'polite' });
    document.body.append(el);
  }
  el.className = 'toast ' + tipo + ' visivel';
  limpar(el).append(icone(tipo === 'erro' ? 'alerta' : 'ok', 18), h('span', {}, mensagem));
  clearTimeout(temporizador);
  temporizador = setTimeout(() => el.classList.remove('visivel'), 4200);
}

// ---- modal
export function modal(titulo, conteudo, { largo = false } = {}) {
  const fundo = h('div', { class: 'modal-fundo' });
  const fechar = () => { fundo.remove(); document.removeEventListener('keydown', teclar); document.body.classList.remove('sem-rolagem'); };
  const teclar = (e) => { if (e.key === 'Escape') fechar(); };
  const caixa = h('div', { class: 'modal' + (largo ? ' largo' : ''), role: 'dialog', 'aria-modal': 'true', 'aria-label': titulo },
    h('header', {}, h('h2', {}, titulo), h('button', { class: 'icone-botao', 'aria-label': 'Fechar', onclick: fechar }, icone('fechar'))),
    h('div', { class: 'modal-corpo' }, conteudo));
  fundo.append(caixa);
  fundo.addEventListener('mousedown', (e) => { if (e.target === fundo) fechar(); });
  document.addEventListener('keydown', teclar);
  document.body.append(fundo);
  document.body.classList.add('sem-rolagem');
  const primeiro = caixa.querySelector('input, select, button.primario');
  if (primeiro) primeiro.focus();
  return { fechar, caixa };
}

// ---- dica flutuante: qualquer elemento com data-dica="texto" ganha uma
let dica;
export function iniciarDicas() {
  dica = h('div', { id: 'dica', role: 'tooltip' });
  document.body.append(dica);
  const mover = (e) => {
    const alvo = e.target.closest?.('[data-dica]');
    if (!alvo) { dica.classList.remove('visivel'); return; }
    dica.textContent = alvo.dataset.dica;
    dica.classList.add('visivel');
    const r = dica.getBoundingClientRect();
    let x = e.clientX + 14, y = e.clientY + 16;
    if (x + r.width > innerWidth - 8) x = e.clientX - r.width - 14;
    if (y + r.height > innerHeight - 8) y = e.clientY - r.height - 14;
    dica.style.transform = `translate(${Math.max(8, x)}px, ${Math.max(8, y)}px)`;
  };
  document.addEventListener('pointermove', mover);
  document.addEventListener('pointerleave', () => dica.classList.remove('visivel'));
  document.addEventListener('scroll', () => dica.classList.remove('visivel'), true);
}

export function esqueleto(linhas = 3) {
  return h('div', { class: 'esqueleto' }, Array.from({ length: linhas }, () => h('div', { class: 'linha' })));
}

export function vazio(texto, detalhe) {
  return h('div', { class: 'vazio' }, h('strong', {}, texto), detalhe && h('span', {}, detalhe));
}

export function erroNaTela(e) {
  return h('div', { class: 'erro-tela', role: 'alert' }, icone('alerta', 22), h('div', {},
    h('strong', {}, 'Não foi possível carregar'), h('span', {}, e?.message || 'Tente novamente.')));
}

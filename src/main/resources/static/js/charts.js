// Gráficos em SVG puro. Cores vêm de variáveis CSS (--s1...), então claro/escuro troca sem redesenhar.
import { svg, h, reais, compacto, mesCurto, numero } from './ui.js';

const DIAS = ['seg', 'ter', 'qua', 'qui', 'sex', 'sáb', 'dom'];

function raiz(largura, altura, rotulo) {
  return svg('svg', { viewBox: `0 0 ${largura} ${altura}`, class: 'grafico' + (largura > 500 ? ' largo' : ''), role: 'img', 'aria-label': rotulo,
    preserveAspectRatio: 'xMidYMid meet' });
}

/** Receita por mês: duas barras (faturado e recebido), base no zero, dica ao passar o mouse. */
export function barrasPorMes(meses) {
  const L = 640, A = 260, mE = 46, mD = 8, mT = 14, mB = 30;
  const s = raiz(L, A, 'Faturado e recebido por mês');
  const max = Math.max(1, ...meses.flatMap((m) => [Number(m.faturado), Number(m.recebido)]));
  const passo = nice(max);
  const topo = Math.ceil(max / passo) * passo;
  const y = (v) => mT + (A - mT - mB) * (1 - v / topo);
  for (let v = 0; v <= topo; v += passo) {
    s.append(svg('line', { x1: mE, x2: L - mD, y1: y(v), y2: y(v), class: v === 0 ? 'eixo' : 'grade' }));
    s.append(svg('text', { x: mE - 8, y: y(v) + 4, class: 'rotulo-eixo', 'text-anchor': 'end' }, compacto(v).replace('R$ ', '')));
  }
  const larg = (L - mE - mD) / meses.length;
  const barra = Math.min(26, larg * 0.32);
  meses.forEach((m, i) => {
    const cx = mE + larg * i + larg / 2;
    const parcial = i === meses.length - 1; // o mês corrente ainda está sendo recebido: barra mais clara
    [[Number(m.faturado), 's1', -barra - 1, 'Faturado'], [Number(m.recebido), 's3', 1, 'Recebido']].forEach(([v, cor, dx, nome]) => {
      const h0 = Math.max(0, y(0) - y(v));
      s.append(svg('path', { d: topoArredondado(cx + dx, y(v), barra, h0), style: `fill:var(--${cor});${parcial && nome === 'Recebido' ? 'fill-opacity:.55' : ''}`,
        'data-dica': `${nome} em ${mesCurto(m.mes)}: ${reais(v)}` }));
    });
    s.append(svg('text', { x: cx, y: A - 10, class: 'rotulo-eixo', 'text-anchor': 'middle' }, mesCurto(m.mes) + (parcial ? ' (parcial)' : '')));
    const falta = Number(m.faturado) - Number(m.recebido);
    s.append(svg('rect', { x: mE + larg * i, y: mT, width: larg, height: A - mT - mB, fill: 'transparent',
      'data-dica': `${mesCurto(m.mes)}: faturado ${reais(m.faturado)} · recebido ${reais(m.recebido)} · em aberto ${reais(falta)}` }));
  });
  return s;
}

function topoArredondado(x, y, w, alt) {
  if (alt <= 0) return '';
  const r = Math.min(4, w / 2, alt);
  return `M${x},${y + alt} V${y + r} Q${x},${y} ${x + r},${y} H${x + w - r} Q${x + w},${y} ${x + w},${y + r} V${y + alt} Z`;
}

function nice(max) {
  const bruto = max / 4;
  const p = 10 ** Math.floor(Math.log10(bruto));
  const f = bruto / p;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * p;
}

/** Mapa de calor 7 x 19 (5h às 23h): uma cor só (azul), do claro ao escuro, para a magnitude. */
export function mapaDeCalor(mapa) {
  const h0 = 5, h1 = 23, L = 640, A = 230, mE = 34, mT = 6, mB = 22;
  const s = raiz(L, A, 'Entradas por dia da semana e hora, últimos 28 dias');
  const colunas = h1 - h0 + 1;
  const cw = (L - mE) / colunas, ch = (A - mT - mB) / 7;
  const max = Math.max(1, mapa.maximo);
  mapa.valores.forEach((linha, d) => {
    s.append(svg('text', { x: mE - 6, y: mT + ch * d + ch / 2 + 4, class: 'rotulo-eixo', 'text-anchor': 'end' }, DIAS[d]));
    for (let hh = h0; hh <= h1; hh++) {
      const v = linha[hh];
      const nivel = v / max;
      s.append(svg('rect', { x: mE + cw * (hh - h0) + 1, y: mT + ch * d + 1, width: cw - 2, height: ch - 2, rx: 3,
        class: 'celula', style: `fill:var(--s1);fill-opacity:${(0.06 + nivel * 0.94).toFixed(2)}`,
        'data-dica': `${DIAS[d]}, ${String(hh).padStart(2, '0')}h: ${numero(v)} entradas` }));
    }
  });
  for (let hh = h0; hh <= h1; hh += 2) {
    s.append(svg('text', { x: mE + cw * (hh - h0) + cw / 2, y: A - 6, class: 'rotulo-eixo', 'text-anchor': 'middle' }, `${hh}h`));
  }
  return s;
}

/** Barras horizontais com o valor escrito ao lado (poucas categorias: rótulo direto, sem legenda). */
export function barrasHorizontais(itens, { cor = 's1', formato = numero } = {}) {
  const max = Math.max(1, ...itens.map((i) => i.valor));
  return h('div', { class: 'hbarras' }, itens.map((i) => h('div', { class: 'hbarra', 'data-dica': `${i.rotulo}: ${i.texto ?? formato(i.valor)}` },
    h('span', { class: 'hb-rotulo' }, i.rotulo),
    h('span', { class: 'hb-trilho' }, h('span', { class: 'hb-barra', style: `width:${(i.valor / max) * 100}%;background:var(--${i.cor || cor})` })),
    h('span', { class: 'hb-valor' }, i.texto ?? formato(i.valor)))));
}

/** Barras das últimas 8 semanas (frequência do aluno). */
export function barrasSemanais(valores) {
  const L = 320, A = 90, base = 72, max = Math.max(1, ...valores);
  const s = raiz(L, A, 'Visitas por semana nas últimas 8 semanas');
  const larg = L / valores.length;
  valores.forEach((v, i) => {
    const alt = (v / max) * (base - 8);
    const rotulo = i === valores.length - 1 ? 'esta' : `-${valores.length - 1 - i}`;
    s.append(svg('path', { d: topoArredondado(larg * i + 7, base - alt, larg - 14, Math.max(alt, v ? 2 : 0)), style: 'fill:var(--s1)',
      'data-dica': `Semana ${rotulo === 'esta' ? 'atual' : 'há ' + (valores.length - 1 - i)}: ${v} visitas` }));
    s.append(svg('line', { x1: 0, x2: L, y1: base, y2: base, class: 'eixo' }));
    s.append(svg('text', { x: larg * i + larg / 2, y: A - 6, class: 'rotulo-eixo', 'text-anchor': 'middle' }, rotulo));
    if (v) s.append(svg('text', { x: larg * i + larg / 2, y: base - alt - 5, class: 'rotulo-valor', 'text-anchor': 'middle' }, v));
  });
  return s;
}

/** Linha simples de uma medida só (peso ou gordura), com marcadores e dica. */
export function linha(pontos, { cor = 's1', unidade = '', casas = 1 } = {}) {
  const L = 320, A = 150, mE = 36, mD = 12, mT = 14, mB = 26;
  const s = raiz(L, A, 'Evolução');
  if (!pontos.length) return s;
  const vs = pontos.map((p) => p.y);
  let lo = Math.min(...vs), hi = Math.max(...vs);
  if (hi - lo < 1) { lo -= 1; hi += 1; }
  const folga = (hi - lo) * 0.2;
  lo -= folga; hi += folga;
  const x = (i) => pontos.length === 1 ? (mE + L - mD) / 2 : mE + (L - mE - mD) * (i / (pontos.length - 1));
  const y = (v) => mT + (A - mT - mB) * (1 - (v - lo) / (hi - lo));
  for (let k = 0; k < 3; k++) {
    const v = lo + ((hi - lo) * k) / 2;
    s.append(svg('line', { x1: mE, x2: L - mD, y1: y(v), y2: y(v), class: 'grade' }));
    s.append(svg('text', { x: mE - 6, y: y(v) + 4, class: 'rotulo-eixo', 'text-anchor': 'end' }, v.toFixed(0)));
  }
  s.append(svg('polyline', { points: pontos.map((p, i) => `${x(i)},${y(p.y)}`).join(' '), fill: 'none',
    style: `stroke:var(--${cor})`, 'stroke-width': 2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round' }));
  pontos.forEach((p, i) => {
    s.append(svg('circle', { cx: x(i), cy: y(p.y), r: 4.5, style: `fill:var(--${cor})`, class: 'marcador',
      'data-dica': `${p.rotulo}: ${p.y.toFixed(casas).replace('.', ',')} ${unidade}` }));
    s.append(svg('text', { x: x(i), y: A - 8, class: 'rotulo-eixo', 'text-anchor': 'middle' }, p.rotulo));
  });
  return s;
}

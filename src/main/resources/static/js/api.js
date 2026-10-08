// Cliente da API. Cada resposta guarda quantas consultas SQL custou (cabeçalhos X-Consultas-Sql e X-Tempo-Ms).

export const ultima = { consultas: null, ms: null, caminho: '', requisicao: null };
const ouvintes = new Set();
export const aoMedir = (fn) => ouvintes.add(fn);

export class ErroDaApi extends Error {
  constructor(status, titulo, detalhe) {
    super(detalhe || titulo);
    this.status = status;
    this.titulo = titulo;
  }
}

function tokenCsrf() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : null;
}

async function chamar(metodo, caminho, corpo) {
  const opcoes = { method: metodo, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
  if (metodo !== 'GET') {
    const t = tokenCsrf();
    if (t) opcoes.headers['X-XSRF-TOKEN'] = t;
  }
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  let resp;
  try {
    resp = await fetch(caminho, opcoes);
  } catch {
    throw new ErroDaApi(0, 'Sem conexão', 'Não foi possível falar com o servidor.');
  }
  const consultas = resp.headers.get('X-Consultas-Sql');
  if (consultas != null) {
    Object.assign(ultima, { consultas: Number(consultas), ms: Number(resp.headers.get('X-Tempo-Ms')),
      caminho: `${metodo} ${caminho.split('?')[0]}`, requisicao: resp.headers.get('X-Requisicao') });
    ouvintes.forEach((fn) => fn(ultima));
  }
  if (resp.status === 204) return null;
  const texto = await resp.text();
  const json = texto ? JSON.parse(texto) : null;
  if (resp.status === 401 && !caminho.startsWith('/api/auth/')) window.dispatchEvent(new Event('sessao-expirada'));
  if (!resp.ok) throw new ErroDaApi(resp.status, json?.title || 'Erro', json?.detail || json?.title);
  return json;
}

export const api = {
  get: (c) => chamar('GET', c),
  post: (c, corpo = {}) => chamar('POST', c, corpo),
  put: (c, corpo) => chamar('PUT', c, corpo),
  apagar: (c) => chamar('DELETE', c),
};

export const consulta = (params) => {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== '' && v != null) q.set(k, v);
  const s = q.toString();
  return s ? '?' + s : '';
};

// Dados de referência (unidades, planos) carregados uma vez.
let ref;
export function referencias() {
  ref ??= Promise.all([api.get('/api/unidades'), api.get('/api/planos')]).then(([unidades, planos]) => ({ unidades, planos }));
  return ref;
}

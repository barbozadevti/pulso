// Estado global mínimo: unidade escolhida (vale para painel, risco e alunos) e tema.

function ler(chave, padrao) {
  try { return localStorage.getItem(chave) ?? padrao; } catch { return padrao; }
}
function gravar(chave, valor) {
  try { valor == null ? localStorage.removeItem(chave) : localStorage.setItem(chave, valor); } catch { /* navegação privada */ }
}

const ouvintes = new Set();

export const estado = {
  unidadeId: ler('pulso.unidade', '') || '',
  tema: ler('pulso.tema', ''),
  definirUnidade(id) {
    this.unidadeId = id || '';
    gravar('pulso.unidade', this.unidadeId || null);
    ouvintes.forEach((f) => f(this.unidadeId));
  },
  aoMudarUnidade(fn) { ouvintes.add(fn); return () => ouvintes.delete(fn); },
  alternarTema() {
    const escuro = document.documentElement.dataset.tema === 'escuro' ||
      (!document.documentElement.dataset.tema && matchMedia('(prefers-color-scheme: dark)').matches);
    this.tema = escuro ? 'claro' : 'escuro';
    document.documentElement.dataset.tema = this.tema;
    gravar('pulso.tema', this.tema);
  },
  aplicarTema() { if (this.tema) document.documentElement.dataset.tema = this.tema; },
};

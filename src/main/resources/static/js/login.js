// Tela de entrada. Mostra as contas de demonstração para quem está avaliando o projeto (dados fictícios).
import { h, limpar, icone } from './ui.js';
import { api } from './api.js';

const ICONE_DO_PERFIL = { DIRETORIA: 'painel', GERENTE: 'risco', RECEPCAO: 'catraca', ALUNO: 'alunos' };
const DESCRICAO = {
  DIRETORIA: 'Rede inteira: painel, risco, laboratório',
  GERENTE: 'Só a unidade dele: painel, risco, retenção',
  RECEPCAO: 'Balcão da unidade: catraca, cadastro, cobrança',
  ALUNO: 'Só o próprio cadastro e as reservas de aula',
};

export async function mostrarLogin(raiz, aoEntrar) {
  limpar(raiz);
  const email = h('input', { class: 'entrada', type: 'email', name: 'login', autocomplete: 'username', required: true, placeholder: 'voce@empresa.com' });
  const senha = h('input', { class: 'entrada', type: 'password', name: 'senha', autocomplete: 'current-password', required: true, placeholder: 'Senha' });
  const erro = h('p', { class: 'login-erro', role: 'alert' });
  const botao = h('button', { class: 'botao primario', type: 'submit', style: 'width:100%' }, 'Entrar');
  const contas = h('div', { class: 'contas-demo' });

  async function entrar(login, pass) {
    botao.disabled = true; erro.textContent = '';
    try {
      await api.get('/api/auth/csrf'); // garante o cookie do token CSRF antes do POST
      const sessao = await api.post('/api/auth/entrar', { login, senha: pass });
      await api.get('/api/auth/csrf'); // o token troca junto com a sessão
      aoEntrar(sessao);
    } catch (e) {
      erro.textContent = e.message;
      botao.disabled = false;
      senha.value = '';
      senha.focus();
    }
  }

  const form = h('form', { class: 'login-form', novalidate: true, onsubmit: (e) => { e.preventDefault(); entrar(email.value, senha.value); } },
    h('label', { class: 'campo' }, h('span', {}, 'E-mail'), email),
    h('label', { class: 'campo' }, h('span', {}, 'Senha'), senha),
    erro, botao);

  raiz.append(h('main', { class: 'login' },
    h('div', { class: 'login-cartao' },
      h('div', { class: 'login-marca' }, h('span', { class: 'logo' }, logo()), h('div', {}, h('h1', {}, 'Pulso'), h('p', { class: 'mudo' }, 'Gestão de redes de academias'))),
      form),
    h('section', { class: 'login-cartao demo' },
      h('header', {}, h('h2', {}, 'Conhecendo o projeto?'), h('p', { class: 'mudo' }, 'Entre com uma conta de demonstração. Os dados são fictícios e cada perfil enxerga só o que lhe cabe.')),
      contas)));

  try {
    const lista = await api.get('/api/auth/demo');
    limpar(contas).append(...lista.map((c) => h('button', { class: 'conta', type: 'button', onclick: () => entrar(c.login, c.senha) },
      h('span', { class: 'conta-icone' }, icone(ICONE_DO_PERFIL[c.perfil] || 'alunos', 20)),
      h('span', { class: 'conta-texto' }, h('strong', {}, `${c.rotulo} · ${c.nome}`), h('small', {}, DESCRICAO[c.perfil] + (c.perfil === 'GERENTE' || c.perfil === 'RECEPCAO' ? '' : '')), h('small', { class: 'mudo' }, c.login)),
      icone('seta', 18))));
    if (!lista.length) contas.parentElement.hidden = true;
  } catch { contas.parentElement.hidden = true; }
  email.focus();
}

function logo() {
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

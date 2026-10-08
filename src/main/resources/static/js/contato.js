// Registro de contato de retenção: o passo que fecha o ciclo "o sistema avisa, a equipe age, o resultado volta".
import { h, modal, toast } from './ui.js';
import { api } from './api.js';

export const CANAIS = { LIGACAO: 'Ligação', WHATSAPP: 'WhatsApp', PRESENCIAL: 'Presencial' };
export const RESULTADOS = {
  VOLTOU_A_TREINAR: ['ok', 'Voltou a treinar'],
  PROMETEU_VOLTAR: ['atencao', 'Prometeu voltar'],
  SEM_RESPOSTA: ['', 'Sem resposta'],
  VAI_CANCELAR: ['critico', 'Vai cancelar'],
};

export function registrarContato({ alunoId, nome, aoSalvar }) {
  const canal = h('select', { class: 'entrada' }, Object.entries(CANAIS).map(([v, t]) => h('option', { value: v }, t)));
  const resultado = h('select', { class: 'entrada' }, Object.entries(RESULTADOS).map(([v, [, t]]) => h('option', { value: v, selected: v === 'PROMETEU_VOLTAR' }, t)));
  const obs = h('input', { class: 'entrada', maxlength: 200, placeholder: 'Ex.: estava viajando, volta na segunda' });
  const m = modal('Contato com ' + nome.split(' ')[0], h('div', { class: 'pilha' },
    h('p', { class: 'suave' }, 'Registre o que foi feito. A nota de risco do momento fica guardada para medir se a abordagem funciona.'),
    h('label', { class: 'campo' }, h('span', {}, 'Como foi o contato'), canal),
    h('label', { class: 'campo' }, h('span', {}, 'Resultado'), resultado),
    h('label', { class: 'campo' }, h('span', {}, 'Observação (opcional)'), obs),
    h('div', { class: 'modal-acoes' }, h('button', { class: 'botao', type: 'button', onclick: () => m.fechar() }, 'Cancelar'),
      h('button', { class: 'botao primario', type: 'button', onclick: async () => {
        try {
          await api.post(`/api/alunos/${alunoId}/contatos`, { canal: canal.value, resultado: resultado.value, observacao: obs.value || null });
          m.fechar();
          toast('Contato registrado.');
          aoSalvar?.();
        } catch (e) { toast(e.message, 'erro'); }
      } }, 'Registrar'))));
}

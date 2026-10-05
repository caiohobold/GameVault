import { cliente } from './cliente';
import type { Avaliacao, Pagina } from '../tipos/api';

export interface DadosAvaliacao {
  jogoId: number;
  nota: number;
  comentario: string;
}

export async function listarMinhas(): Promise<Pagina<Avaliacao>> {
  const { data } = await cliente.get<Pagina<Avaliacao>>('/avaliacoes', { params: { size: 50 } });
  return data;
}

export async function criar(dados: DadosAvaliacao): Promise<Avaliacao> {
  const { data } = await cliente.post<Avaliacao>('/avaliacoes', dados);
  return data;
}

export async function excluir(id: number): Promise<void> {
  await cliente.delete(`/avaliacoes/${id}`);
}

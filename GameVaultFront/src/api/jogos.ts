import { cliente } from './cliente';
import type { Avaliacao, Jogo, Pagina, Venda } from '../tipos/api';

export interface FiltroJogos {
  titulo?: string;
  categoriaId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DadosJogo {
  titulo: string;
  descricao: string;
  preco: number;
  dataLancamento: string | null;
  publicadoraId?: number;
  categoriaIds: number[];
}

export async function listar(filtro: FiltroJogos = {}): Promise<Pagina<Jogo>> {
  const { data } = await cliente.get<Pagina<Jogo>>('/jogos', { params: filtro });
  return data;
}

export async function buscarPorId(id: number): Promise<Jogo> {
  const { data } = await cliente.get<Jogo>(`/jogos/${id}`);
  return data;
}

export async function listarAvaliacoes(id: number, page = 0): Promise<Pagina<Avaliacao>> {
  const { data } = await cliente.get<Pagina<Avaliacao>>(`/jogos/${id}/avaliacoes`, { params: { page } });
  return data;
}

export async function listarMeus(page = 0): Promise<Pagina<Jogo>> {
  const { data } = await cliente.get<Pagina<Jogo>>('/jogos/meus', { params: { page, size: 50 } });
  return data;
}

export async function resumirVendas(): Promise<Pagina<Venda>> {
  const { data } = await cliente.get<Pagina<Venda>>('/jogos/meus/vendas', { params: { size: 50 } });
  return data;
}

export async function criar(dados: DadosJogo): Promise<Jogo> {
  const { data } = await cliente.post<Jogo>('/jogos', dados);
  return data;
}

export async function atualizar(id: number, dados: DadosJogo): Promise<Jogo> {
  const { data } = await cliente.put<Jogo>(`/jogos/${id}`, dados);
  return data;
}

export async function desativar(id: number): Promise<void> {
  await cliente.delete(`/jogos/${id}`);
}

export async function reativar(id: number): Promise<Jogo> {
  const { data } = await cliente.patch<Jogo>(`/jogos/${id}/reativar`);
  return data;
}

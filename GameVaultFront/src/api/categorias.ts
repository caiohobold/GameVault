import { cliente } from './cliente';
import type { Categoria, Pagina } from '../tipos/api';

export async function listar(): Promise<Categoria[]> {
  const { data } = await cliente.get<Pagina<Categoria>>('/categorias', { params: { size: 100 } });
  return data.content;
}

export async function criar(dados: { nome: string; descricao: string }): Promise<Categoria> {
  const { data } = await cliente.post<Categoria>('/categorias', dados);
  return data;
}

export async function excluir(id: number): Promise<void> {
  await cliente.delete(`/categorias/${id}`);
}

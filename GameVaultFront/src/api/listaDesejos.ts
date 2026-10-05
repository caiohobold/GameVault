import { cliente } from './cliente';
import type { ItemListaDesejos, Pagina } from '../tipos/api';

export async function listar(): Promise<Pagina<ItemListaDesejos>> {
  const { data } = await cliente.get<Pagina<ItemListaDesejos>>('/lista-desejos', { params: { size: 50 } });
  return data;
}

export async function adicionar(jogoId: number): Promise<ItemListaDesejos> {
  const { data } = await cliente.post<ItemListaDesejos>('/lista-desejos', { jogoId });
  return data;
}

export async function remover(id: number): Promise<void> {
  await cliente.delete(`/lista-desejos/${id}`);
}

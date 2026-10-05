import { cliente } from './cliente';
import type { Pagina, Pedido, StatusPedido } from '../tipos/api';

export async function listarMeus(status?: StatusPedido): Promise<Pagina<Pedido>> {
  const { data } = await cliente.get<Pagina<Pedido>>('/pedidos/meus', { params: { status, size: 50 } });
  return data;
}

export async function criar(jogoIds: number[]): Promise<Pedido> {
  const { data } = await cliente.post<Pedido>('/pedidos', {
    itens: jogoIds.map((jogoId) => ({ jogoId })),
  });
  return data;
}

export async function pagar(id: number): Promise<Pedido> {
  const { data } = await cliente.post<Pedido>(`/pedidos/${id}/pagar`);
  return data;
}

export async function cancelar(id: number): Promise<Pedido> {
  const { data } = await cliente.post<Pedido>(`/pedidos/${id}/cancelar`);
  return data;
}

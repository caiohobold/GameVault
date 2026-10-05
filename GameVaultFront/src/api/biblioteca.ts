import { cliente } from './cliente';
import type { ItemBiblioteca, Pagina } from '../tipos/api';

export async function listarMinha(): Promise<Pagina<ItemBiblioteca>> {
  const { data } = await cliente.get<Pagina<ItemBiblioteca>>('/biblioteca/minha', { params: { size: 50 } });
  return data;
}

export async function registrarHoras(id: number, horasJogadas: number): Promise<ItemBiblioteca> {
  const { data } = await cliente.patch<ItemBiblioteca>(`/biblioteca/${id}/horas`, { horasJogadas });
  return data;
}

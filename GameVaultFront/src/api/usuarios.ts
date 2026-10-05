import { cliente } from './cliente';
import type { Pagina, Papel, Usuario } from '../tipos/api';

export async function listar(nome?: string, role?: Papel): Promise<Pagina<Usuario>> {
  const { data } = await cliente.get<Pagina<Usuario>>('/usuarios', { params: { nome, role, size: 50 } });
  return data;
}

export async function alterarAtivo(id: number, ativo: boolean): Promise<Usuario> {
  const { data } = await cliente.patch<Usuario>(`/usuarios/${id}/ativo`, { ativo });
  return data;
}

export async function buscarPorId(id: number): Promise<Usuario> {
  const { data } = await cliente.get<Usuario>(`/usuarios/${id}`);
  return data;
}

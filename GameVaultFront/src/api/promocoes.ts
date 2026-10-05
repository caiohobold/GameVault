import { cliente } from './cliente';
import type { Pagina, Promocao } from '../tipos/api';

export interface DadosPromocao {
  jogoId: number;
  percentualDesconto: number;
  dataInicio: string;
  dataFim: string;
}

export async function listarVigentes(): Promise<Promocao[]> {
  const { data } = await cliente.get<Pagina<Promocao>>('/promocoes', { params: { size: 100 } });
  return data.content.filter((promocao) => promocao.vigente);
}

export async function criar(dados: DadosPromocao): Promise<Promocao> {
  const { data } = await cliente.post<Promocao>('/promocoes', dados);
  return data;
}

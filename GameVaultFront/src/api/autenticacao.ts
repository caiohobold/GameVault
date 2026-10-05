import { cliente } from './cliente';
import type { Autenticacao, Papel } from '../tipos/api';

export interface DadosLogin {
  email: string;
  senha: string;
}

export interface DadosCadastro {
  nome: string;
  email: string;
  senha: string;
  role: Exclude<Papel, 'ADMIN'>;
}

export async function entrar(dados: DadosLogin): Promise<Autenticacao> {
  const { data } = await cliente.post<Autenticacao>('/auth/login', dados);
  return data;
}

export async function registrar(dados: DadosCadastro): Promise<Autenticacao> {
  const { data } = await cliente.post<Autenticacao>('/auth/registrar', dados);
  return data;
}

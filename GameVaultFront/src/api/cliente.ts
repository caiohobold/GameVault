import axios, { AxiosError } from 'axios';
import type { ErroApi } from '../tipos/api';

const CHAVE_TOKEN = 'gamevault.token';

export const cliente = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

export function lerToken(): string | null {
  return localStorage.getItem(CHAVE_TOKEN);
}

export function gravarToken(token: string): void {
  localStorage.setItem(CHAVE_TOKEN, token);
}

export function apagarToken(): void {
  localStorage.removeItem(CHAVE_TOKEN);
}

cliente.interceptors.request.use((configuracao) => {
  const token = lerToken();

  if (token) {
    configuracao.headers.Authorization = `Bearer ${token}`;
  }

  return configuracao;
});

let aoExpirarSessao: (() => void) | null = null;

export function registrarTratamentoDeSessaoExpirada(acao: () => void): void {
  aoExpirarSessao = acao;
}

cliente.interceptors.response.use(
  (resposta) => resposta,
  (erro: AxiosError<ErroApi>) => {
    const rotaDeLogin = erro.config?.url?.startsWith('/auth');

    if (erro.response?.status === 401 && !rotaDeLogin) {
      apagarToken();
      aoExpirarSessao?.();
    }

    return Promise.reject(erro);
  },
);

export function mensagemDoErro(erro: unknown, padrao = 'Não foi possível completar a operação'): string {
  if (!axios.isAxiosError(erro)) {
    return padrao;
  }

  const corpo = erro.response?.data as ErroApi | undefined;

  if (corpo?.campos) {
    const campos = Object.values(corpo.campos);

    if (campos.length > 0) {
      return campos.join('. ');
    }
  }

  if (corpo?.mensagem) {
    return corpo.mensagem;
  }

  if (erro.code === 'ERR_NETWORK') {
    return 'Não foi possível falar com a API. Confira se o backend está rodando em ' + cliente.defaults.baseURL;
  }

  return padrao;
}

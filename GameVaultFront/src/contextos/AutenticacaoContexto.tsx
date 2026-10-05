import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import {
  apagarToken,
  gravarToken,
  lerToken,
  registrarTratamentoDeSessaoExpirada,
} from '../api/cliente';
import * as apiAutenticacao from '../api/autenticacao';
import type { DadosCadastro, DadosLogin } from '../api/autenticacao';
import type { Papel, Usuario } from '../tipos/api';

const CHAVE_USUARIO = 'gamevault.usuario';

interface ValorContexto {
  usuario: Usuario | null;
  autenticado: boolean;
  carregando: boolean;
  entrar: (dados: DadosLogin) => Promise<void>;
  cadastrar: (dados: DadosCadastro) => Promise<void>;
  sair: () => void;
  atualizarUsuario: (usuario: Usuario) => void;
  temPapel: (...papeis: Papel[]) => boolean;
}

export const AutenticacaoContexto = createContext<ValorContexto | null>(null);

function lerUsuarioSalvo(): Usuario | null {
  const bruto = localStorage.getItem(CHAVE_USUARIO);

  if (!bruto || !lerToken()) {
    return null;
  }

  try {
    return JSON.parse(bruto) as Usuario;
  } catch {
    return null;
  }
}

export function ProvedorAutenticacao({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(lerUsuarioSalvo);
  const [carregando, setCarregando] = useState(false);

  const sair = useCallback(() => {
    apagarToken();
    localStorage.removeItem(CHAVE_USUARIO);
    setUsuario(null);
  }, []);

  useEffect(() => {
    registrarTratamentoDeSessaoExpirada(sair);
  }, [sair]);

  const guardarSessao = useCallback((token: string, usuarioAutenticado: Usuario) => {
    gravarToken(token);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuarioAutenticado));
    setUsuario(usuarioAutenticado);
  }, []);

  const entrar = useCallback(
    async (dados: DadosLogin) => {
      setCarregando(true);

      try {
        const resposta = await apiAutenticacao.entrar(dados);
        guardarSessao(resposta.token, resposta.usuario);
      } finally {
        setCarregando(false);
      }
    },
    [guardarSessao],
  );

  const cadastrar = useCallback(
    async (dados: DadosCadastro) => {
      setCarregando(true);

      try {
        const resposta = await apiAutenticacao.registrar(dados);
        guardarSessao(resposta.token, resposta.usuario);
      } finally {
        setCarregando(false);
      }
    },
    [guardarSessao],
  );

  const atualizarUsuario = useCallback((atualizado: Usuario) => {
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(atualizado));
    setUsuario(atualizado);
  }, []);

  const temPapel = useCallback(
    (...papeis: Papel[]) => (usuario ? papeis.includes(usuario.role) : false),
    [usuario],
  );

  const valor = useMemo<ValorContexto>(
    () => ({
      usuario,
      autenticado: usuario !== null,
      carregando,
      entrar,
      cadastrar,
      sair,
      atualizarUsuario,
      temPapel,
    }),
    [usuario, carregando, entrar, cadastrar, sair, atualizarUsuario, temPapel],
  );

  return <AutenticacaoContexto.Provider value={valor}>{children}</AutenticacaoContexto.Provider>;
}

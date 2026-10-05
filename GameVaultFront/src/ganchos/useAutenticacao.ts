import { useContext } from 'react';
import { AutenticacaoContexto } from '../contextos/AutenticacaoContexto';

export function useAutenticacao() {
  const contexto = useContext(AutenticacaoContexto);

  if (!contexto) {
    throw new Error('useAutenticacao precisa estar dentro de ProvedorAutenticacao');
  }

  return contexto;
}

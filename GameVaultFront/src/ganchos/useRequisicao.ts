import { useCallback, useEffect, useRef, useState } from 'react';
import { mensagemDoErro } from '../api/cliente';

interface Estado<T> {
  dados: T | null;
  carregando: boolean;
  erro: string | null;
}

export function useRequisicao<T>(buscar: () => Promise<T>, dependencias: unknown[] = []) {
  const [estado, setEstado] = useState<Estado<T>>({ dados: null, carregando: true, erro: null });
  const buscarAtual = useRef(buscar);
  buscarAtual.current = buscar;

  const executar = useCallback(async () => {
    setEstado((atual) => ({ ...atual, carregando: true, erro: null }));

    try {
      const dados = await buscarAtual.current();
      setEstado({ dados, carregando: false, erro: null });
    } catch (erro) {
      setEstado({ dados: null, carregando: false, erro: mensagemDoErro(erro) });
    }
  }, dependencias);

  useEffect(() => {
    void executar();
  }, [executar]);

  return { ...estado, recarregar: executar };
}

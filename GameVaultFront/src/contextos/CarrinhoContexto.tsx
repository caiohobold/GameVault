import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { Jogo } from '../tipos/api';

const CHAVE_CARRINHO = 'gamevault.carrinho';

interface ValorContexto {
  itens: Jogo[];
  quantidade: number;
  contem: (jogoId: number) => boolean;
  adicionar: (jogo: Jogo) => void;
  remover: (jogoId: number) => void;
  limpar: () => void;
}

export const CarrinhoContexto = createContext<ValorContexto | null>(null);

function lerCarrinhoSalvo(): Jogo[] {
  const bruto = localStorage.getItem(CHAVE_CARRINHO);

  if (!bruto) {
    return [];
  }

  try {
    return JSON.parse(bruto) as Jogo[];
  } catch {
    return [];
  }
}

export function ProvedorCarrinho({ children }: { children: ReactNode }) {
  const [itens, setItens] = useState<Jogo[]>(lerCarrinhoSalvo);

  useEffect(() => {
    localStorage.setItem(CHAVE_CARRINHO, JSON.stringify(itens));
  }, [itens]);

  const contem = useCallback((jogoId: number) => itens.some((jogo) => jogo.id === jogoId), [itens]);

  const adicionar = useCallback((jogo: Jogo) => {
    setItens((atuais) => (atuais.some((item) => item.id === jogo.id) ? atuais : [...atuais, jogo]));
  }, []);

  const remover = useCallback((jogoId: number) => {
    setItens((atuais) => atuais.filter((jogo) => jogo.id !== jogoId));
  }, []);

  const limpar = useCallback(() => setItens([]), []);

  const valor = useMemo<ValorContexto>(
    () => ({ itens, quantidade: itens.length, contem, adicionar, remover, limpar }),
    [itens, contem, adicionar, remover, limpar],
  );

  return <CarrinhoContexto.Provider value={valor}>{children}</CarrinhoContexto.Provider>;
}

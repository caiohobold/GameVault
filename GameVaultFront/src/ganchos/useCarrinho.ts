import { useContext } from 'react';
import { CarrinhoContexto } from '../contextos/CarrinhoContexto';

export function useCarrinho() {
  const contexto = useContext(CarrinhoContexto);

  if (!contexto) {
    throw new Error('useCarrinho precisa estar dentro de ProvedorCarrinho');
  }

  return contexto;
}

import { Link } from 'react-router-dom';
import { CapaJogo } from './CapaJogo';
import { EstrelasNota } from './EstrelasNota';
import { PrecoJogo } from './PrecoJogo';
import type { Jogo } from '../tipos/api';

interface Props {
  jogo: Jogo;
  percentualDesconto?: number;
}

export function CartaoJogo({ jogo, percentualDesconto }: Props) {
  return (
    <Link to={`/jogos/${jogo.id}`} className="cartao-jogo">
      <CapaJogo id={jogo.id} titulo={jogo.titulo} />
      <div className="corpo">
        <div className="titulo">{jogo.titulo}</div>
        <div className="categorias">
          {jogo.categorias.map((categoria) => categoria.nome).join(' · ') || 'Sem categoria'}
        </div>
        <div className="rodape">
          {percentualDesconto ? (
            <span className="selo-desconto">-{percentualDesconto}%</span>
          ) : (
            <EstrelasNota nota={jogo.notaMedia} />
          )}
          <PrecoJogo preco={jogo.preco} percentualDesconto={percentualDesconto} />
        </div>
      </div>
    </Link>
  );
}

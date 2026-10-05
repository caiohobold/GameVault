import { aplicarDesconto, formatarMoeda } from '../utilitarios/formatos';

interface Props {
  preco: number;
  percentualDesconto?: number;
  tamanho?: number;
}

export function PrecoJogo({ preco, percentualDesconto, tamanho }: Props) {
  if (!percentualDesconto) {
    return (
      <div className="preco" style={{ fontSize: tamanho }}>
        {formatarMoeda(preco)}
      </div>
    );
  }

  return (
    <div>
      <div className="preco-antigo">{formatarMoeda(preco)}</div>
      <div className="preco" style={{ fontSize: tamanho }}>
        {formatarMoeda(aplicarDesconto(preco, percentualDesconto))}
      </div>
    </div>
  );
}

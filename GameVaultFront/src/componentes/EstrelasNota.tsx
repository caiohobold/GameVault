import { formatarNota } from '../utilitarios/formatos';

interface Props {
  nota: number | null | undefined;
  total?: number;
}

export function EstrelasNota({ nota, total }: Props) {
  const valor = Number(nota ?? 0);

  if (valor === 0) {
    return <span style={{ color: 'var(--texto-fraco)', fontSize: 13 }}>Sem avaliações</span>;
  }

  return (
    <span className="nota">
      ★ {formatarNota(valor)}
      {total !== undefined && (
        <span style={{ color: 'var(--texto-fraco)', fontWeight: 400 }}> ({total})</span>
      )}
    </span>
  );
}

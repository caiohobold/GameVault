import { indiceDeCapa } from '../utilitarios/formatos';

interface Props {
  id: number;
  titulo: string;
  altura?: string;
  fonte?: string;
}

export function CapaJogo({ id, titulo, altura, fonte }: Props) {
  return (
    <div
      className={`capa capa-${indiceDeCapa(id)}`}
      style={{ aspectRatio: altura ?? '16 / 9', fontSize: fonte }}
    >
      {titulo}
    </div>
  );
}

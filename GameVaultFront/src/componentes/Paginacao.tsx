interface Props {
  paginaAtual: number;
  totalPaginas: number;
  aoMudar: (pagina: number) => void;
}

export function Paginacao({ paginaAtual, totalPaginas, aoMudar }: Props) {
  if (totalPaginas <= 1) {
    return null;
  }

  return (
    <nav className="paginacao">
      <button type="button" disabled={paginaAtual === 0} onClick={() => aoMudar(paginaAtual - 1)}>
        ‹
      </button>
      <span>
        Página {paginaAtual + 1} de {totalPaginas}
      </span>
      <button
        type="button"
        disabled={paginaAtual >= totalPaginas - 1}
        onClick={() => aoMudar(paginaAtual + 1)}
      >
        ›
      </button>
    </nav>
  );
}

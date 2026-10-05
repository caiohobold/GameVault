interface Props {
  tipo: 'erro' | 'sucesso' | 'aviso';
  children: React.ReactNode;
}

export function Mensagem({ tipo, children }: Props) {
  return <div className={tipo}>{children}</div>;
}

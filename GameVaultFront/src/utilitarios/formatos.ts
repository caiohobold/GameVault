const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' });

export function formatarMoeda(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined) {
    return MOEDA.format(0);
  }

  return MOEDA.format(Number(valor));
}

export function formatarData(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }

  return DATA.format(new Date(iso));
}

export function formatarNota(nota: number | null | undefined): string {
  if (nota === null || nota === undefined || Number(nota) === 0) {
    return 'Sem avaliações';
  }

  return Number(nota).toFixed(1).replace('.', ',');
}

export function aplicarDesconto(preco: number, percentual: number): number {
  return Number((preco - preco * (percentual / 100)).toFixed(2));
}

export function indiceDeCapa(id: number): number {
  return (id % 8) + 1;
}

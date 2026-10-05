export type Papel = 'ADMIN' | 'PUBLICADORA' | 'USUARIO';

export type StatusPedido = 'PENDENTE' | 'PAGO' | 'CANCELADO';

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface UsuarioResumo {
  id: number;
  nome: string;
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  role: Papel;
  saldo: number;
  dataCadastro: string;
  ativo: boolean;
}

export interface Categoria {
  id: number;
  nome: string;
  descricao: string | null;
}

export interface JogoResumo {
  id: number;
  titulo: string;
  preco: number;
  notaMedia: number | null;
  ativo: boolean;
}

export interface Jogo {
  id: number;
  titulo: string;
  descricao: string | null;
  preco: number;
  dataLancamento: string | null;
  publicadora: UsuarioResumo;
  notaMedia: number | null;
  totalAvaliacoes: number;
  ativo: boolean;
  categorias: Categoria[];
}

export interface Promocao {
  id: number;
  jogo: JogoResumo;
  percentualDesconto: number;
  dataInicio: string;
  dataFim: string;
  ativa: boolean;
  vigente: boolean;
}

export interface ItemPedido {
  id: number;
  jogo: JogoResumo;
  precoUnitario: number;
  descontoAplicado: number;
  subtotal: number;
}

export interface Pedido {
  id: number;
  usuario: UsuarioResumo;
  dataPedido: string;
  status: StatusPedido;
  valorTotal: number;
  itens: ItemPedido[];
}

export interface ItemBiblioteca {
  id: number;
  usuario: UsuarioResumo;
  jogo: JogoResumo;
  dataAquisicao: string;
  horasJogadas: number;
}

export interface Avaliacao {
  id: number;
  usuario: UsuarioResumo;
  jogo: JogoResumo;
  nota: number;
  comentario: string | null;
  dataAvaliacao: string;
}

export interface ItemListaDesejos {
  id: number;
  usuario: UsuarioResumo;
  jogo: JogoResumo;
  dataAdicao: string;
}

export interface Venda {
  jogoId: number;
  titulo: string;
  quantidadeVendida: number;
  valorArrecadado: number;
}

export interface Autenticacao {
  token: string;
  tipo: string;
  expiraEm: string;
  usuario: Usuario;
}

export interface ErroApi {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  path: string;
  campos?: Record<string, string>;
}

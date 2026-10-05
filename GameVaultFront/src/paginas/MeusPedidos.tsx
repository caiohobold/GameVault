import { mensagemDoErro } from '../api/cliente';
import * as apiPedidos from '../api/pedidos';
import { Carregando } from '../componentes/Carregando';
import { Mensagem } from '../componentes/Mensagem';
import { useRequisicao } from '../ganchos/useRequisicao';
import { formatarData, formatarMoeda } from '../utilitarios/formatos';

export function MeusPedidos() {
  const { dados, carregando, erro, recarregar } = useRequisicao(() => apiPedidos.listarMeus(), []);

  async function cancelar(id: number) {
    try {
      await apiPedidos.cancelar(id);
      recarregar();
    } catch (falha) {
      window.alert(mensagemDoErro(falha));
    }
  }

  return (
    <>
      <h1>Meus pedidos</h1>
      <p className="subtitulo">Histórico de compras na plataforma</p>

      {carregando && <Carregando />}
      {erro && <Mensagem tipo="erro">{erro}</Mensagem>}

      {dados && dados.content.length === 0 && (
        <div className="cartao vazio">Você ainda não fez nenhum pedido.</div>
      )}

      {dados?.content.map((pedido) => (
        <article key={pedido.id} className="cartao painel" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
            <h2 style={{ margin: 0 }}>Pedido #{pedido.id}</h2>
            <span className={`etiqueta-papel status-${pedido.status}`}>{pedido.status}</span>
            <span style={{ marginLeft: 'auto', color: 'var(--texto-fraco)', fontSize: 13 }}>
              {formatarData(pedido.dataPedido)}
            </span>
          </div>

          <table>
            <thead>
              <tr>
                <th>Jogo</th>
                <th>Preço</th>
                <th>Desconto</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {pedido.itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.jogo.titulo}</td>
                  <td>{formatarMoeda(item.precoUnitario)}</td>
                  <td style={{ color: Number(item.descontoAplicado) > 0 ? 'var(--verde)' : undefined }}>
                    {Number(item.descontoAplicado) > 0 ? `− ${formatarMoeda(item.descontoAplicado)}` : '—'}
                  </td>
                  <td>{formatarMoeda(item.subtotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="total grande">
            <span>Total</span>
            <span>{formatarMoeda(pedido.valorTotal)}</span>
          </div>

          {pedido.status === 'PENDENTE' && (
            <button
              type="button"
              className="botao perigo pequeno"
              style={{ marginTop: 12 }}
              onClick={() => cancelar(pedido.id)}
            >
              Cancelar pedido
            </button>
          )}
        </article>
      ))}
    </>
  );
}

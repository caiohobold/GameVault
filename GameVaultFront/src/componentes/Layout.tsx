import { Outlet } from 'react-router-dom';
import { Cabecalho } from './Cabecalho';

export function Layout() {
  return (
    <>
      <Cabecalho />
      <main className="conteudo">
        <Outlet />
      </main>
    </>
  );
}

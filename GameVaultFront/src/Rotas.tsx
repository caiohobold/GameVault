import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './componentes/Layout';
import { RotaProtegida } from './componentes/RotaProtegida';
import { Biblioteca } from './paginas/Biblioteca';
import { Cadastro } from './paginas/Cadastro';
import { Carrinho } from './paginas/Carrinho';
import { Catalogo } from './paginas/Catalogo';
import { DetalheJogo } from './paginas/DetalheJogo';
import { Login } from './paginas/Login';
import { MeusPedidos } from './paginas/MeusPedidos';
import { PainelAdmin } from './paginas/PainelAdmin';
import { PainelPublicadora } from './paginas/PainelPublicadora';

export function Rotas() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Catalogo />} />
          <Route path="jogos/:id" element={<DetalheJogo />} />
          <Route path="login" element={<Login />} />
          <Route path="cadastro" element={<Cadastro />} />

          <Route element={<RotaProtegida />}>
            <Route path="carrinho" element={<Carrinho />} />
            <Route path="biblioteca" element={<Biblioteca />} />
            <Route path="pedidos" element={<MeusPedidos />} />
          </Route>

          <Route element={<RotaProtegida papeis={['PUBLICADORA', 'ADMIN']} />}>
            <Route path="publicadora" element={<PainelPublicadora />} />
          </Route>

          <Route element={<RotaProtegida papeis={['ADMIN']} />}>
            <Route path="admin" element={<PainelAdmin />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

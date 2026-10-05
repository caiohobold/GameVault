import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import type { Papel } from '../tipos/api';

interface Props {
  papeis?: Papel[];
}

export function RotaProtegida({ papeis }: Props) {
  const { autenticado, usuario } = useAutenticacao();
  const localizacao = useLocation();

  if (!autenticado) {
    return <Navigate to="/login" state={{ de: localizacao.pathname }} replace />;
  }

  if (papeis && usuario && !papeis.includes(usuario.role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

package br.edu.unesc.gamevault.mapper;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.UsuarioResponse;
import br.edu.unesc.gamevault.dto.response.UsuarioResumoResponse;
import br.edu.unesc.gamevault.entity.Usuario;

@Component
public class UsuarioMapper {
    public UsuarioResponse paraResposta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getSaldo(),
                usuario.getDataCadastro(),
                usuario.getAtivo());
    }

    public UsuarioResumoResponse paraResumo(Usuario usuario) {
        return new UsuarioResumoResponse(usuario.getId(), usuario.getNome());
    }
}

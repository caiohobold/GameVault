package br.edu.unesc.gamevault.ia.cadastro;

/** Idiomas para os quais o assistente traduz a descrição do jogo. */
public enum IdiomaTraducao {
    EN("en", "inglês"),
    ES("es", "espanhol");

    private final String codigo;
    private final String nome;

    IdiomaTraducao(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }
}

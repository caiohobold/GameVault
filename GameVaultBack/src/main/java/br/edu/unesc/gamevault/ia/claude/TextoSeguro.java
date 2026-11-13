package br.edu.unesc.gamevault.ia.claude;

/**
 * Prepara texto vindo de usuários para entrar no prompt dentro de tags XML.
 * Escapar os sinais de menor e maior impede que alguém feche a tag no meio do
 * comentário e escreva "instruções" fora dela.
 */
public final class TextoSeguro {
    private TextoSeguro() {
    }

    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Monta {@code <tag>texto escapado</tag>}. */
    public static String tag(String nome, String texto) {
        return "<" + nome + ">" + escapar(texto) + "</" + nome + ">";
    }
}

package model;


public class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }


    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuario() {
        return usuarioActual;
    }


    public static boolean hayUsuario() {
        return usuarioActual != null;
    }


    public static boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.esAdministrador();
    }


    public static void cerrar() {
        usuarioActual = null;
    }
}
package model;

public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String clave;
    private String rol;
    private String estado;

    public Usuario() {
        this.estado = "Activo";
    }

    public Usuario(int idUsuario, String nombreUsuario, String clave, String rol) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.clave = clave;
        this.rol = rol;
        this.estado = "Activo";
    }

    public Usuario(int idUsuario, String nombre, String apellido, String nombreUsuario,
                   String clave, String rol, String estado) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.nombreUsuario = nombreUsuario;
        this.clave = clave;
        this.rol = rol;
        this.estado = estado;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNombreCompleto() {
        String n = nombre == null ? "" : nombre;
        String a = apellido == null ? "" : apellido;
        String completo = (n + " " + a).trim();
        return completo.isEmpty() ? nombreUsuario : completo;
    }

    public boolean esAdministrador() {
        return rol != null && rol.equalsIgnoreCase("Administrador");
    }

    public boolean estaActivo() {
        return estado == null || estado.equalsIgnoreCase("Activo");
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
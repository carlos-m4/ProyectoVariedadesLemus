package DAO;

import model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class UsuarioDAO {

    private static final String COLUMNAS =
            "id_usuario, nombre, apellido, rol, usuario, `contraseña`, estado";

    public Usuario validarLogin(String usuario, String clave) {
        String sql = "SELECT " + COLUMNAS + " FROM usuarios "
                + "WHERE usuario = ? AND `contraseña` = ? AND estado = 'Activo'";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, cifrar(clave));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerFila(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al validar el login: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM usuarios ORDER BY nombre, apellido";
        return consultar(sql);
    }

    public List<Usuario> buscar(String texto, String rol) {
        String sql = "SELECT " + COLUMNAS + " FROM usuarios "
                + "WHERE (nombre LIKE ? OR apellido LIKE ? OR usuario LIKE ?) "
                + "AND (? = '' OR rol = ?) "
                + "ORDER BY nombre, apellido";

        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        String rolFiltro = (rol == null || rol.equalsIgnoreCase("Todos")) ? "" : rol;

        return consultar(sql, patron, patron, patron, rolFiltro, rolFiltro);
    }

    public boolean insertar(Usuario u) {
        String sql = "INSERT INTO usuarios (nombre, apellido, rol, usuario, `contraseña`, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getRol());
            ps.setString(4, u.getNombreUsuario());
            ps.setString(5, cifrar(u.getClave()));
            ps.setString(6, u.getEstado() == null ? "Activo" : u.getEstado());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar el usuario: " + e.getMessage());
            return false;
        }
    }

    // Cambia los datos de un usuario que ya existe.
    // Si la clave viene vacía, no se toca la contraseña vieja.
    public boolean actualizar(Usuario u) {
        boolean cambiaClave = u.getClave() != null && !u.getClave().isBlank();

        String sql = "UPDATE usuarios SET nombre = ?, apellido = ?, rol = ?, usuario = ?"
                + (cambiaClave ? ", `contraseña` = ?" : "")
                + " WHERE id_usuario = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, u.getNombre());
            ps.setString(i++, u.getApellido());
            ps.setString(i++, u.getRol());
            ps.setString(i++, u.getNombreUsuario());
            if (cambiaClave) {
                ps.setString(i++, cifrar(u.getClave()));
            }
            ps.setInt(i, u.getIdUsuario());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar el usuario: " + e.getMessage());
            return false;
        }
    }

    // Apaga o prende a un usuario ("Activo" / "Inactivo"). No lo borra.
    public boolean cambiarEstado(int idUsuario, String estado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar el estado: " + e.getMessage());
            return false;
        }
    }

    // ¿Ya alguien usa ese nombre de usuario?
    public boolean existeUsuario(String usuario) {
        String sql = "SELECT 1 FROM usuarios WHERE usuario = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al revisar el usuario: " + e.getMessage());
            // Si no pudimos revisar, decimos que sí existe para no guardar repetidos.
            return true;
        }
    }

    // ---------------------------------------------------------
    // Convierte la contraseña en un código secreto (SHA-256).
    // "admin123" se vuelve una fila larga de letras y números.
    // Es el mismo código que hace SHA2() en MySQL.
    // ---------------------------------------------------------
    public static String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));

            // Pasamos cada pedacito a letras y números (hexadecimal).
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Java no tiene SHA-256", e);
        }
    }

    // ---------------------------------------------------------
    // Ayudantes de adentro (solo los usa esta clase)
    // ---------------------------------------------------------

    // Hace una búsqueda y arma una lista con las fichas que encuentre.
    private List<Usuario> consultar(String sql, String... valores) {
        List<Usuario> lista = new ArrayList<>();

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < valores.length; i++) {
                ps.setString(i + 1, valores[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerFila(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar usuarios: " + e.getMessage());
        }
        return lista;
    }

    // Lee una fila de la tabla y la vuelve una ficha Usuario.
    private Usuario leerFila(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("usuario"),
                rs.getString("contraseña"),
                rs.getString("rol"),
                rs.getString("estado")
        );
    }
}
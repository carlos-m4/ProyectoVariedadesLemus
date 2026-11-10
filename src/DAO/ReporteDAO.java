package DAO;

import model.Reporte;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {

    private static final String COLUMNAS =
            "id_reporte, cliente, tipo, descripcion, monto, estado, fecha, id_venta";

    // Guarda una incidencia nueva. Devuelve true si salió bien.
    public boolean insertar(Reporte r) {
        String sql = "INSERT INTO reporte (cliente, tipo, descripcion, monto, estado, fecha, id_venta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, r.getCliente().trim());
            ps.setString(2, r.getTipo());
            ps.setString(3, r.getDescripcion().trim());
            ps.setDouble(4, r.getMonto());
            ps.setString(5, r.getEstado() == null ? "Pendiente" : r.getEstado());
            ps.setDate(6, Date.valueOf(r.getFecha() == null ? LocalDate.now() : r.getFecha()));

            // Si no habla de ninguna venta, guardamos NULL (nada).
            if (r.getIdVenta() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, r.getIdVenta());
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar la incidencia: " + e.getMessage());
            return false;
        }
    }

    public List<Reporte> listarPorRango(LocalDate desde, LocalDate hasta, String estado) {
        String filtro = (estado == null || estado.equalsIgnoreCase("Todos")) ? "" : estado;

        String sql = "SELECT " + COLUMNAS + " FROM reporte "
                + "WHERE fecha BETWEEN ? AND ? "
                + "AND (? = '' OR estado = ?) "
                + "ORDER BY fecha DESC, id_reporte DESC";

        List<Reporte> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.setString(3, filtro);
            ps.setString(4, filtro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerFila(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar incidencias: " + e.getMessage());
        }
        return lista;
    }

    public List<Reporte> buscarPorCliente(String cliente) {
        String sql = "SELECT " + COLUMNAS + " FROM reporte "
                + "WHERE cliente LIKE ? "
                + "ORDER BY fecha DESC, id_reporte DESC";

        List<Reporte> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + (cliente == null ? "" : cliente.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerFila(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar incidencias: " + e.getMessage());
        }
        return lista;
    }

    public boolean cambiarEstado(int idReporte, String estado) {
        String sql = "UPDATE reporte SET estado = ? WHERE id_reporte = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idReporte);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar el estado: " + e.getMessage());
            return false;
        }
    }

    public int contarPendientes() {
        String sql = "SELECT COUNT(*) FROM reporte WHERE estado = 'Pendiente'";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Error al contar incidencias: " + e.getMessage());
            return 0;
        }
    }

    private Reporte leerFila(ResultSet rs) throws SQLException {
        // getObject nos deja saber si id_venta está vacío (null).
        Object venta = rs.getObject("id_venta");

        return new Reporte(
                rs.getInt("id_reporte"),
                rs.getString("cliente"),
                rs.getString("tipo"),
                rs.getString("descripcion"),
                rs.getDouble("monto"),
                rs.getString("estado"),
                rs.getDate("fecha").toLocalDate(),
                venta == null ? null : ((Number) venta).intValue()
        );
    }
}
package DAO;

import model.DetalleVenta;
import model.Venta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VentaDAO {

    public int registrarVenta(Venta venta) throws SQLException {
        if (venta.getDetalles().isEmpty()) {
            throw new SQLException("El carrito está vacío.");
        }

        String sqlVenta = "INSERT INTO venta (fecha, total, cliente, id_usuario) VALUES (?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_venta (cantidad, total, id_venta, id_producto) VALUES (?, ?, ?, ?)";

        // Solo resta si hay suficientes. Si no hay, no cambia nada.
        String sqlStock = "UPDATE inventario SET stock = stock - ? "
                + "WHERE id_producto = ? AND stock >= ?";

        venta.calcularTotal();

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int idVenta;

                try (PreparedStatement ps = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setDate(1, Date.valueOf(venta.getFecha()));
                    ps.setDouble(2, venta.getTotal());
                    ps.setString(3, venta.getCliente() == null || venta.getCliente().isBlank()
                            ? "Cliente final" : venta.getCliente().trim());
                    ps.setInt(4, venta.getIdUsuario());
                    ps.executeUpdate();

                    try (ResultSet llaves = ps.getGeneratedKeys()) {
                        if (!llaves.next()) {
                            throw new SQLException("La base no devolvió el número de la venta.");
                        }
                        idVenta = llaves.getInt(1);
                    }
                }

                try (PreparedStatement psDet = con.prepareStatement(sqlDetalle);
                     PreparedStatement psStock = con.prepareStatement(sqlStock)) {

                    for (DetalleVenta d : venta.getDetalles()) {
                        // Restamos de la bodega.
                        psStock.setInt(1, d.getCantidad());
                        psStock.setInt(2, d.getIdProducto());
                        psStock.setInt(3, d.getCantidad());

                        if (psStock.executeUpdate() == 0) {
                            throw new SQLException("No hay suficiente stock de: " + d.getNombreProducto());
                        }

                        psDet.setInt(1, d.getCantidad());
                        psDet.setDouble(2, d.getTotal());
                        psDet.setInt(3, idVenta);
                        psDet.setInt(4, d.getIdProducto());
                        psDet.executeUpdate();

                        d.setIdVenta(idVenta);
                    }
                }

                con.commit();
                venta.setIdVenta(idVenta);
                return idVenta;

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public double totalDelDia(LocalDate fecha, Integer idUsuario) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM venta WHERE fecha = ?"
                + (idUsuario != null ? " AND id_usuario = ?" : "");
        return numeroDelDia(sql, fecha, idUsuario);
    }

    public int cantidadDelDia(LocalDate fecha, Integer idUsuario) {
        String sql = "SELECT COUNT(*) FROM venta WHERE fecha = ?"
                + (idUsuario != null ? " AND id_usuario = ?" : "");
        return (int) numeroDelDia(sql, fecha, idUsuario);
    }

    public boolean existe(int idVenta) {
        String sql = "SELECT 1 FROM venta WHERE id_venta = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar la venta: " + e.getMessage());
            return false;
        }
    }

    public List<Venta> listarPorRango(LocalDate desde, LocalDate hasta, boolean conDetalles) {
        String sql = "SELECT v.id_venta, v.fecha, v.total, v.cliente, v.id_usuario, "
                + "       CONCAT(u.nombre, ' ', u.apellido) AS atendio "
                + "FROM venta v "
                + "JOIN usuarios u ON u.id_usuario = v.id_usuario "
                + "WHERE v.fecha BETWEEN ? AND ? "
                + "ORDER BY v.fecha, v.id_venta";

        Map<Integer, Venta> ventas = new LinkedHashMap<>();

        try (Connection con = Conexion.getConexion()) {

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Venta v = new Venta(
                                rs.getInt("id_venta"),
                                rs.getDate("fecha").toLocalDate(),
                                rs.getDouble("total"),
                                rs.getString("cliente"),
                                rs.getInt("id_usuario"));
                        v.setNombreUsuario(rs.getString("atendio"));
                        ventas.put(v.getIdVenta(), v);
                    }
                }
            }

            if (conDetalles && !ventas.isEmpty()) {
                cargarDetallesPorRango(con, desde, hasta, ventas);
            }

        } catch (SQLException e) {
            System.err.println("Error al listar ventas: " + e.getMessage());
        }
        return new ArrayList<>(ventas.values());
    }

    public List<DetalleVenta> listarDetalles(int idVenta) {
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, p.nombre, d.cantidad, d.total "
                + "FROM detalle_venta d "
                + "JOIN producto p ON p.id_producto = d.id_producto "
                + "WHERE d.id_venta = ? "
                + "ORDER BY d.id_detalle";

        List<DetalleVenta> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerDetalle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al traer el detalle: " + e.getMessage());
        }
        return lista;
    }

    private void cargarDetallesPorRango(Connection con, LocalDate desde, LocalDate hasta,
                                        Map<Integer, Venta> ventas) throws SQLException {
        String sql = "SELECT d.id_detalle, d.id_venta, d.id_producto, p.nombre, d.cantidad, d.total "
                + "FROM detalle_venta d "
                + "JOIN venta v ON v.id_venta = d.id_venta "
                + "JOIN producto p ON p.id_producto = d.id_producto "
                + "WHERE v.fecha BETWEEN ? AND ? "
                + "ORDER BY d.id_venta, d.id_detalle";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta d = leerDetalle(rs);
                    Venta duena = ventas.get(d.getIdVenta());
                    if (duena != null) {
                        duena.getDetalles().add(d);
                    }
                }
            }
        }
    }

    private double numeroDelDia(String sql, LocalDate fecha, Integer idUsuario) {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            if (idUsuario != null) {
                ps.setInt(2, idUsuario);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error en el resumen del día: " + e.getMessage());
            return 0;
        }
    }

    private DetalleVenta leerDetalle(ResultSet rs) throws SQLException {
        return new DetalleVenta(
                rs.getInt("id_detalle"),
                rs.getInt("id_venta"),
                rs.getInt("id_producto"),
                rs.getString("nombre"),
                rs.getInt("cantidad"),
                rs.getDouble("total")
        );
    }
}
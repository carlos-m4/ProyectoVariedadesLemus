package DAO;

import model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class ProductoDAO {

    private static final String SELECT_BASE =
            "SELECT p.id_producto, p.nombre, p.descripcion, p.categoria, p.precio, p.estado, "
                    + "       i.id_inventario, i.precio_costo, i.stock, i.stock_minimo "
                    + "FROM producto p "
                    + "LEFT JOIN inventario i ON i.id_producto = p.id_producto ";

    public List<Producto> listar(boolean soloActivos) {
        String sql = SELECT_BASE
                + (soloActivos ? "WHERE p.estado = 'Activo' " : "")
                + "ORDER BY p.id_producto";
        return consultar(sql);
    }

    public List<Producto> buscar(String texto, String categoria, boolean soloActivos) {
        String limpio = texto == null ? "" : texto.trim();
        String cat = (categoria == null || categoria.equalsIgnoreCase("Todas")) ? "" : categoria.trim();


        int codigo = -1;
        if (limpio.matches("\\d+")) {
            codigo = Integer.parseInt(limpio);
        }

        String sql = SELECT_BASE
                + "WHERE (p.nombre LIKE ? OR p.id_producto = ?) "
                + "AND (? = '' OR p.categoria = ?) "
                + (soloActivos ? "AND p.estado = 'Activo' " : "")
                + "ORDER BY p.nombre";

        List<Producto> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + limpio + "%");
            ps.setInt(2, codigo);
            ps.setString(3, cat);
            ps.setString(4, cat);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerFila(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar productos: " + e.getMessage());
        }
        return lista;
    }

    public Producto buscarPorId(int idProducto) {
        String sql = SELECT_BASE + "WHERE p.id_producto = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerFila(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar el producto: " + e.getMessage());
        }
        return null;
    }

    public List<String> listarCategorias() {
        String sql = "SELECT DISTINCT categoria FROM producto ORDER BY categoria";
        List<String> lista = new ArrayList<>();

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("categoria"));
            }
        } catch (SQLException e) {
            System.err.println("Error al traer categorías: " + e.getMessage());
        }
        return lista;
    }

    public List<Producto> listarStockBajo() {
        String sql = SELECT_BASE
                + "WHERE p.estado = 'Activo' AND i.stock <= i.stock_minimo "
                + "ORDER BY i.stock ASC";
        return consultar(sql);
    }

    public int contarActivos() {
        return contar("SELECT COUNT(*) FROM producto WHERE estado = 'Activo'");
    }

    public int contarUnidades() {
        return contar("SELECT COALESCE(SUM(i.stock), 0) FROM inventario i "
                + "JOIN producto p ON p.id_producto = i.id_producto WHERE p.estado = 'Activo'");
    }

    public boolean existeNombre(String nombre) {
        String sql = "SELECT 1 FROM producto WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?))";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al revisar el nombre: " + e.getMessage());
            // Si no pudimos revisar, decimos que sí existe para no guardar repetidos.
            return true;
        }
    }

    public int insertar(Producto p) {
        try (Connection con = Conexion.getConexion()) {

            con.setAutoCommit(false);
            try {
                int nuevoId = insertarEn(con, p);
                con.commit();
                return nuevoId;
            } catch (SQLException e) {
                // Algo falló: deshacemos todo como si nada hubiera pasado.
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al guardar el producto: " + e.getMessage());
            return -1;
        }
    }

    public boolean actualizar(Producto p) {
        String sqlProducto = "UPDATE producto SET nombre = ?, descripcion = ?, categoria = ?, precio = ? "
                + "WHERE id_producto = ?";

        String sqlInventario = "INSERT INTO inventario (precio_costo, stock, stock_minimo, id_producto) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE precio_costo = VALUES(precio_costo), "
                + "stock = VALUES(stock), stock_minimo = VALUES(stock_minimo)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(sqlProducto);
                 PreparedStatement ps2 = con.prepareStatement(sqlInventario)) {

                ps1.setString(1, p.getNombre());
                ps1.setString(2, vacioANull(p.getDescripcion()));
                ps1.setString(3, p.getCategoria());
                ps1.setDouble(4, p.getPrecio());
                ps1.setInt(5, p.getIdProducto());
                ps1.executeUpdate();

                ps2.setDouble(1, p.getPrecioCosto());
                ps2.setInt(2, p.getStock());
                ps2.setInt(3, p.getStockMinimo());
                ps2.setInt(4, p.getIdProducto());
                ps2.executeUpdate();

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar el producto: " + e.getMessage());
            return false;
        }
    }

    public boolean cambiarEstado(int idProducto, String estado) {
        String sql = "UPDATE producto SET estado = ? WHERE id_producto = ?";

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar el estado: " + e.getMessage());
            return false;
        }
    }

    public int[] importar(List<Producto> productos) {
        int guardados = 0;
        int omitidos = 0;

        List<String> vistos = new ArrayList<>();

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                for (Producto p : productos) {
                    String clave = p.getNombre().trim().toLowerCase();

                    if (vistos.contains(clave) || existeNombreEn(con, p.getNombre())) {
                        omitidos++;
                        continue;
                    }
                    insertarEn(con, p);
                    vistos.add(clave);
                    guardados++;
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al importar: " + e.getMessage());

            return new int[]{0, productos.size()};
        }
        return new int[]{guardados, omitidos};
    }

    private int insertarEn(Connection con, Producto p) throws SQLException {
        String sqlProducto = "INSERT INTO producto (nombre, descripcion, categoria, precio, estado) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlInventario = "INSERT INTO inventario (precio_costo, stock, stock_minimo, id_producto) "
                + "VALUES (?, ?, ?, ?)";

        int nuevoId;

        try (PreparedStatement ps = con.prepareStatement(sqlProducto, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre().trim());
            ps.setString(2, vacioANull(p.getDescripcion()));
            ps.setString(3, p.getCategoria().trim());
            ps.setDouble(4, p.getPrecio());
            ps.setString(5, p.getEstado() == null ? "Activo" : p.getEstado());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                if (!llaves.next()) {
                    throw new SQLException("La base no devolvió el código del producto.");
                }
                nuevoId = llaves.getInt(1);
            }
        }

        try (PreparedStatement ps = con.prepareStatement(sqlInventario)) {
            ps.setDouble(1, p.getPrecioCosto());
            ps.setInt(2, p.getStock());
            ps.setInt(3, p.getStockMinimo());
            ps.setInt(4, nuevoId);
            ps.executeUpdate();
        }

        p.setIdProducto(nuevoId);
        return nuevoId;
    }

    private boolean existeNombreEn(Connection con, String nombre) throws SQLException {
        String sql = "SELECT 1 FROM producto WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Producto> consultar(String sql) {
        List<Producto> lista = new ArrayList<>();

        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(leerFila(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar productos: " + e.getMessage());
        }
        return lista;
    }

    private int contar(String sql) {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Error al contar: " + e.getMessage());
            return 0;
        }
    }

    private Producto leerFila(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id_producto"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("categoria"),
                rs.getDouble("precio"),
                rs.getString("estado"),
                rs.getInt("id_inventario"),
                rs.getDouble("precio_costo"),
                rs.getInt("stock"),
                rs.getInt("stock_minimo")
        );
    }

    private String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
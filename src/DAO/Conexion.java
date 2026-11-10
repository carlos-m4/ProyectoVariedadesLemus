package DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/variedadeslemus_db"
            + "?serverTimezone=America/El_Salvador"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&characterEncoding=UTF-8";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Conexion() {
    }

    public static Connection getConexion() throws SQLException {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de MySQL. Revisa la carpeta lib.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }


    public static boolean probarConexion() {
        try (Connection con = getConexion()) {
            return con != null && con.isValid(2);
        } catch (SQLException e) {
            System.err.println("No se pudo conectar: " + e.getMessage());
            return false;
        }
    }
}
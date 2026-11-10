package controller;

import DAO.ProductoDAO;
import DAO.VentaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Producto;
import model.Sesion;
import model.Usuario;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PanelPrincipalController {


    public static final String VISTA_INVENTARIO = "/vista/Inventario.fxml";
    public static final String VISTA_VENTAS = "/vista/RegistoVenta.fxml";
    public static final String VISTA_REPORTES = "/vista/Reportes.fxml";
    public static final String VISTA_USUARIOS = "/vista/GestionUsuarios.fxml";
    public static final String VISTA_EMPLEADO = "/vista/PanelPrincipalEmpleado.fxml";


    public static final String ACCION_NUEVO = "nuevo";
    public static final String ACCION_IMPORTAR = "importar";
    public static final String ACCION_INCIDENCIAS = "incidencias";


    private static PanelPrincipalController instancia;

    @FXML private AnchorPane rootPrincipal;
    @FXML private AnchorPane contenidoPrincipal;
    @FXML private AnchorPane menuLateral;
    @FXML private VBox panelResumen;


    @FXML private Button btnInicio;
    @FXML private Button btnInventario;
    @FXML private Button btnVentas;
    @FXML private Button btnReportes;
    @FXML private Button btnUsuarios;
    @FXML private Button btnCerrarSesion;

    @FXML private Label lblBienvenidaName;
    @FXML private Label lblFechaActual;
    @FXML private Label total_inventario_PP;
    @FXML private Label Add_inventario_PP;
    @FXML private Label monto_venta_PP;
    @FXML private Label cant_venta_PP;
    @FXML private Label lblCantStock;
    @FXML private Label name_product_PP;
    @FXML private Label mas_detalles_PP;
    @FXML private Label Add_User_PP;
    @FXML private Label Import_Archivo_PP;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private Node subpanelAbierto;

    private String accionPendiente;

    public static PanelPrincipalController getInstancia() {
        return instancia;
    }

    @FXML
    private void initialize() {
        instancia = this;

        Usuario usuario = Sesion.getUsuario();
        String nombre = usuario == null ? "Administrador" : usuario.getNombre();
        lblBienvenidaName.setText("¡Bienvenido, " + nombre + "!");
        lblFechaActual.setText("📅 " + fechaBonita(LocalDate.now()));

        acomodarMenuSegunRol();
        mostrarInicio();
    }

    @FXML
    void onInicioClick(ActionEvent event) {
        mostrarInicio();
    }

    @FXML
    void onInventarioClick(ActionEvent event) {
        mostrarInventario();
    }

    @FXML
    void onVentasClick(ActionEvent event) {
        mostrarVentas();
    }

    @FXML
    void onrReportesClick(ActionEvent event) {
        mostrarReportes();
    }

    @FXML
    void onUsuariosClick(ActionEvent event) {
        mostrarUsuarios();
    }

    @FXML
    void onCerrarSesionClick(ActionEvent event) {
        if (!Mensajes.confirmar("Cerrar sesión", "¿Seguro que quieres salir del sistema?")) {
            return;
        }
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/vista/Login.fxml"));
            Stage stage = (Stage) rootPrincipal.getScene().getWindow();
            Sesion.cerrar();
            instancia = null;
            stage.setScene(new Scene(login));
            stage.setTitle("Variedades Lemus - Inicio de Sesión");
            stage.setMaximized(true);
        } catch (IOException e) {
            Mensajes.error("Error", "No se pudo volver al inicio de sesión.\n" + Mensajes.causaReal(e));
        }
    }

    @FXML
    void onVentasPP(ActionEvent event) {
        mostrarVentas();
    }

    @FXML
    void onInventarioPP(ActionEvent event) {
        accionPendiente = ACCION_NUEVO;
        mostrarInventario();
    }

    @FXML
    void onrReportesPP(ActionEvent event) {
        mostrarReportes();
    }

    @FXML
    void OnAddUserPP(MouseEvent event) {
        accionPendiente = ACCION_NUEVO;
        mostrarUsuarios();
    }

    @FXML
    void OnImportPP(MouseEvent event) {
        accionPendiente = ACCION_IMPORTAR;
        mostrarInventario();
    }

    @FXML
    void on_detalles_stock_Click(MouseEvent event) {
        List<Producto> bajos = productoDAO.listarStockBajo();

        if (bajos.isEmpty()) {
            Mensajes.info("Stock bajo", "¡Todo en orden! Ningún producto está por debajo de su mínimo.");
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (Producto p : bajos) {
            texto.append("• ").append(p.getNombre())
                    .append(": quedan ").append(p.getStock())
                    .append(" (mínimo ").append(p.getStockMinimo()).append(")\n");
        }

        ButtonType irInventario = new ButtonType("Ir a inventario", ButtonBar.ButtonData.OK_DONE);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerta = new Alert(Alert.AlertType.WARNING, texto.toString(), irInventario, cerrar);
        alerta.setTitle("Stock bajo");
        alerta.setHeaderText(bajos.size() + " producto(s) necesitan reabastecerse");

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == irInventario) {
            mostrarInventario();
        }
    }

    public void mostrarInicio() {
        cerrarSubpanel();
        marcarActivo(btnInicio);

        if (Sesion.esAdministrador()) {
            contenidoPrincipal.getChildren().setAll(panelResumen);
            cargarResumen();
        } else {
            cargarPantalla(VISTA_EMPLEADO);
        }
    }

    public <T> T mostrarInventario() {
        if (!soloAdministrador()) {
            return null;
        }
        marcarActivo(btnInventario);
        return cargarPantalla(VISTA_INVENTARIO);
    }

    public <T> T mostrarVentas() {
        marcarActivo(btnVentas);
        return cargarPantalla(VISTA_VENTAS);
    }

    public <T> T mostrarReportes() {
        marcarActivo(btnReportes);
        return cargarPantalla(VISTA_REPORTES);
    }

    public <T> T mostrarUsuarios() {
        if (!soloAdministrador()) {
            return null;
        }
        marcarActivo(btnUsuarios);
        return cargarPantalla(VISTA_USUARIOS);
    }


    public void setAccionPendiente(String accion) {
        this.accionPendiente = accion;
    }

    public String tomarAccionPendiente() {
        String accion = accionPendiente;
        accionPendiente = null;
        return accion;
    }

    public <T> T cargarPantalla(String ruta) {
        cerrarSubpanel();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent vista = loader.load();

            if (AnchorPane.getLeftAnchor(vista) == null) {
                AnchorPane.setTopAnchor(vista, 0.0);
                AnchorPane.setRightAnchor(vista, 0.0);
                AnchorPane.setBottomAnchor(vista, 0.0);
                AnchorPane.setLeftAnchor(vista, 0.0);
            }
            soltarTamanoFijo(vista);

            contenidoPrincipal.getChildren().setAll(vista);
            return loader.getController();

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            accionPendiente = null;
            Mensajes.error("Error", "No se pudo abrir la pantalla.\n" + Mensajes.causaReal(e));
            return null;
        }
    }

    public <T> T abrirSubpanel(String ruta) {
        cerrarSubpanel();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent vista = loader.load();


            vista.setStyle("-fx-background-color: transparent;");
            if (vista instanceof Region region) {
                region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            }

            StackPane capa = new StackPane(vista);
            capa.getStyleClass().add("capa-oscura");
            AnchorPane.setTopAnchor(capa, 0.0);
            AnchorPane.setRightAnchor(capa, 0.0);
            AnchorPane.setBottomAnchor(capa, 0.0);
            AnchorPane.setLeftAnchor(capa, 0.0);

            capa.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    cerrarSubpanel();
                }
            });

            rootPrincipal.getChildren().add(capa);
            subpanelAbierto = capa;
            capa.requestFocus();
            return loader.getController();

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Mensajes.error("Error", "No se pudo abrir el formulario.\n" + Mensajes.causaReal(e));
            return null;
        }
    }

    public void cerrarSubpanel() {
        if (subpanelAbierto != null) {
            rootPrincipal.getChildren().remove(subpanelAbierto);
            subpanelAbierto = null;
        }
    }

    private void cargarResumen() {
        LocalDate hoy = LocalDate.now();

        total_inventario_PP.setText(String.valueOf(productoDAO.contarActivos()));
        Add_inventario_PP.setText(productoDAO.contarUnidades() + " unidades en bodega");

        monto_venta_PP.setText(Mensajes.dinero(ventaDAO.totalDelDia(hoy, null)));
        int ventas = ventaDAO.cantidadDelDia(hoy, null);
        cant_venta_PP.setText(ventas + (ventas == 1 ? " transacción" : " transacciones"));

        List<Producto> bajos = productoDAO.listarStockBajo();
        lblCantStock.setText(String.valueOf(bajos.size()));

        if (bajos.isEmpty()) {
            name_product_PP.setText("Todo en orden");
        } else if (bajos.size() == 1) {
            name_product_PP.setText(bajos.get(0).getNombre());
        } else {
            name_product_PP.setText(bajos.get(0).getNombre() + " y " + (bajos.size() - 1) + " más");
        }
    }

    private void acomodarMenuSegunRol() {
        if (Sesion.esAdministrador()) {
            return;
        }
        btnInventario.setVisible(false);
        btnUsuarios.setVisible(false);
        btnVentas.setLayoutY(138.0);
        btnReportes.setLayoutY(184.0);
    }

    private boolean soloAdministrador() {
        if (Sesion.esAdministrador()) {
            return true;
        }
        Mensajes.advertencia("Acceso restringido", "Esta sección es solo para el administrador.");
        return false;
    }

    private void marcarActivo(Button activo) {
        for (Button b : new Button[]{btnInicio, btnInventario, btnVentas, btnReportes, btnUsuarios}) {
            b.getStyleClass().remove("activo");
        }
        if (activo != null) {
            activo.getStyleClass().add("activo");
        }
    }

    private void soltarTamanoFijo(Parent vista) {
        if (vista instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }

    private String fechaBonita(LocalDate fecha) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                "EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-SV"));
        String texto = fecha.format(formato);
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
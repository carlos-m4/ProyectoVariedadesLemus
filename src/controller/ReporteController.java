package controller;

import DAO.ProductoDAO;
import DAO.ReporteDAO;
import DAO.VentaDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import model.DetalleVenta;
import model.Producto;
import model.Reporte;
import model.Sesion;
import model.Usuario;
import model.Venta;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReporteController {
    public static final String R_VENTAS = "Ventas";
    public static final String R_STOCK = "Stock bajo";
    public static final String R_HISTORIAL = "Historial de compras";
    public static final String R_INCIDENCIAS = "Incidencias por cliente";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private TableView<Fila> tblReporte;
    @FXML private TableColumn<Fila, String> col1;
    @FXML private TableColumn<Fila, String> col2;
    @FXML private TableColumn<Fila, String> col3;
    @FXML private TableColumn<Fila, String> col4;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    private final ObservableList<Fila> filas = FXCollections.observableArrayList();
    private String tipoGenerado;
    private LocalDate desdeGenerado;
    private LocalDate hastaGenerado;

    public static class Fila {
        private final String c1;
        private final String c2;
        private final String c3;
        private final String c4;
        private final Integer idReporte;

        public Fila(String c1, String c2, String c3, String c4, Integer idReporte) {
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
            this.idReporte = idReporte;
        }

        public String getC1() { return c1; }
        public String getC2() { return c2; }
        public String getC3() { return c3; }
        public String getC4() { return c4; }
        public Integer getIdReporte() { return idReporte; }

        public boolean esTotal() {
            return c1 != null && c1.startsWith("TOTAL");
        }
    }

    @FXML
    private void initialize() {
        col1.setCellValueFactory(new PropertyValueFactory<>("c1"));
        col2.setCellValueFactory(new PropertyValueFactory<>("c2"));
        col3.setCellValueFactory(new PropertyValueFactory<>("c3"));
        col4.setCellValueFactory(new PropertyValueFactory<>("c4"));

        tblReporte.setItems(filas);
        tblReporte.setPlaceholder(new Label("Elige un tipo de reporte y toca Generar"));
        tblReporte.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Fila fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setStyle(!vacio && fila != null && fila.esTotal()
                        ? "-fx-font-weight: bold; -fx-background-color: #e8eef5;" : "");
            }
        });

        if (Sesion.esAdministrador()) {
            cmbTipoReporte.getItems().setAll(R_VENTAS, R_STOCK, R_HISTORIAL, R_INCIDENCIAS);
            configurarCambioDeEstado();
        } else {
            cmbTipoReporte.getItems().setAll(R_INCIDENCIAS);
        }

        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);

        cmbTipoReporte.valueProperty().addListener((obs, antes, ahora) -> {
            boolean sinFechas = R_STOCK.equals(ahora);
            dpDesde.setDisable(sinFechas);
            dpHasta.setDisable(sinFechas);
            filas.clear();
            tipoGenerado = null;
        });

        Platform.runLater(this::revisarAccionPendiente);
    }

    @FXML
    void onGenerarReporte(ActionEvent event) {
        String tipo = cmbTipoReporte.getValue();
        if (tipo == null) {
            Mensajes.advertencia("Reportes", "Primero escoge el tipo de reporte.");
            return;
        }

        LocalDate desde = dpDesde.getValue() == null ? LocalDate.now().withDayOfMonth(1) : dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue() == null ? LocalDate.now() : dpHasta.getValue();
        if (!R_STOCK.equals(tipo) && desde.isAfter(hasta)) {
            Mensajes.advertencia("Reportes", "La fecha \"Desde\" no puede ser después de \"Hasta\".");
            return;
        }

        switch (tipo) {
            case R_VENTAS -> generarVentas(desde, hasta);
            case R_STOCK -> generarStockBajo();
            case R_HISTORIAL -> generarHistorial(desde, hasta);
            default -> generarIncidencias(desde, hasta);
        }

        tipoGenerado = tipo;
        desdeGenerado = desde;
        hastaGenerado = hasta;

        if (filas.isEmpty()) {
            tblReporte.setPlaceholder(new Label("No hay datos para este reporte en las fechas escogidas"));
        }
    }

    @FXML
    void onExportarPDF(ActionEvent event) {
        if (tipoGenerado == null || filas.isEmpty()) {
            Mensajes.advertencia("Exportar", "Primero genera un reporte con datos.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte en PDF");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF (*.pdf)", "*.pdf"));
        selector.setInitialFileName("Reporte_" + tipoGenerado.replace(' ', '_') + "_" + LocalDate.now() + ".pdf");

        File archivo = selector.showSaveDialog(tblReporte.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".pdf");
        }

        String[] encabezados = {col1.getText(), col2.getText(), col3.getText(), col4.getText()};
        double[] proporciones = {col1.getWidth(), col2.getWidth(), col3.getWidth(), col4.getWidth()};
        List<String[]> datos = new ArrayList<>();
        for (Fila f : filas) {
            datos.add(new String[]{f.getC1(), f.getC2(), f.getC3(), f.getC4()});
        }

        try {
            GeneradorPDF.crear(archivo, "Reporte de " + tipoGenerado.toLowerCase(), subtituloPDF(),
                    encabezados, proporciones, datos);
            Mensajes.info("Exportar", "El reporte se guardó en:\n" + archivo.getAbsolutePath());
        } catch (IOException e) {
            Mensajes.error("Exportar", "No se pudo guardar el PDF.\n" + Mensajes.causaReal(e));
        }
    }

    private void generarVentas(LocalDate desde, LocalDate hasta) {
        ponerEncabezados("Ticket / Fecha", "Detalle", "Total", "Estado");
        List<Fila> nuevas = new ArrayList<>();
        double suma = 0;

        List<Venta> ventas = ventaDAO.listarPorRango(desde, hasta, true);
        for (Venta v : ventas) {
            nuevas.add(new Fila(
                    String.format("%06d", v.getIdVenta()) + "  " + v.getFecha().format(FECHA),
                    v.getCantidadProductos() + " producto(s) · atendió " + v.getNombreUsuario(),
                    Mensajes.dinero(v.getTotal()),
                    "Pagada", null));
            suma += v.getTotal();
        }
        if (!ventas.isEmpty()) {
            nuevas.add(new Fila("TOTAL", ventas.size() + " venta(s)", Mensajes.dinero(suma), "", null));
        }
        filas.setAll(nuevas);
    }

    private void generarStockBajo() {
        ponerEncabezados("Código", "Producto", "Existencias", "Estado");
        List<Fila> nuevas = new ArrayList<>();

        for (Producto p : productoDAO.listarStockBajo()) {
            nuevas.add(new Fila(
                    String.valueOf(p.getIdProducto()),
                    p.getNombre() + " (" + p.getCategoria() + ")",
                    p.getStock() + " de mín. " + p.getStockMinimo(),
                    p.getStock() <= 0 ? "Agotado" : "Reabastecer", null));
        }
        filas.setAll(nuevas);
    }

    private void generarHistorial(LocalDate desde, LocalDate hasta) {
        ponerEncabezados("Fecha", "Producto", "Cantidad", "Total (sin IVA)");
        List<Fila> nuevas = new ArrayList<>();
        int unidades = 0;
        double suma = 0;

        for (Venta v : ventaDAO.listarPorRango(desde, hasta, true)) {
            for (DetalleVenta d : v.getDetalles()) {
                nuevas.add(new Fila(
                        v.getFecha().format(FECHA),
                        d.getNombreProducto() + " (ticket " + String.format("%06d", v.getIdVenta()) + ")",
                        d.getCantidad() + " u.",
                        Mensajes.dinero(d.getTotal()), null));
                unidades += d.getCantidad();
                suma += d.getTotal();
            }
        }
        if (!nuevas.isEmpty()) {
            nuevas.add(new Fila("TOTAL", nuevas.size() + " línea(s) vendidas", unidades + " u.", Mensajes.dinero(suma), null));
        }
        filas.setAll(nuevas);
    }

    private void generarIncidencias(LocalDate desde, LocalDate hasta) {
        ponerEncabezados("Fecha", "Cliente / Detalle", "Monto", "Estado");
        List<Fila> nuevas = new ArrayList<>();
        double suma = 0;
        int pendientes = 0;

        List<Reporte> incidencias = reporteDAO.listarPorRango(desde, hasta, "Todos");
        for (Reporte r : incidencias) {
            String fecha = r.getFecha().format(FECHA);
            if (r.getIdVenta() != null) {
                fecha += " · tk " + String.format("%06d", r.getIdVenta());
            }
            nuevas.add(new Fila(
                    fecha,
                    r.getCliente() + " — " + r.getTipo() + ": " + r.getDescripcion(),
                    Mensajes.dinero(r.getMonto()),
                    r.getEstado(), r.getIdReporte()));
            suma += r.getMonto();
            if (r.estaPendiente()) {
                pendientes++;
            }
        }
        if (!incidencias.isEmpty()) {
            nuevas.add(new Fila("TOTAL", incidencias.size() + " incidencia(s)", Mensajes.dinero(suma),
                    pendientes + " pendiente(s)", null));
        }
        filas.setAll(nuevas);
    }

    private void configurarCambioDeEstado() {
        MenuItem pendiente = new MenuItem("Marcar como Pendiente");
        MenuItem resuelto = new MenuItem("Marcar como Resuelto");
        MenuItem rechazado = new MenuItem("Marcar como Rechazado");
        pendiente.setOnAction(e -> cambiarEstado("Pendiente"));
        resuelto.setOnAction(e -> cambiarEstado("Resuelto"));
        rechazado.setOnAction(e -> cambiarEstado("Rechazado"));
        ContextMenu menu = new ContextMenu(pendiente, resuelto, rechazado);

        tblReporte.setOnContextMenuRequested(e -> {
            Fila fila = tblReporte.getSelectionModel().getSelectedItem();
            boolean sePuede = R_INCIDENCIAS.equals(tipoGenerado) && fila != null && fila.getIdReporte() != null;
            if (sePuede) {
                menu.show(tblReporte, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void cambiarEstado(String estado) {
        Fila fila = tblReporte.getSelectionModel().getSelectedItem();
        if (fila == null || fila.getIdReporte() == null) {
            return;
        }
        if (reporteDAO.cambiarEstado(fila.getIdReporte(), estado)) {
            generarIncidencias(desdeGenerado, hastaGenerado);
        } else {
            Mensajes.error("Incidencias", "No se pudo cambiar el estado.");
        }
    }

    private void ponerEncabezados(String t1, String t2, String t3, String t4) {
        col1.setText(t1);
        col2.setText(t2);
        col3.setText(t3);
        col4.setText(t4);
    }

    private String subtituloPDF() {
        Usuario u = Sesion.getUsuario();
        String quien = u == null ? "" : " · Generado por " + u.getNombreCompleto();
        if (R_STOCK.equals(tipoGenerado)) {
            return "Existencias al " + LocalDate.now().format(FECHA) + quien;
        }
        return "Del " + desdeGenerado.format(FECHA) + " al " + hastaGenerado.format(FECHA) + quien;
    }

    private void revisarAccionPendiente() {
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        if (marco != null && PanelPrincipalController.ACCION_INCIDENCIAS.equals(marco.tomarAccionPendiente())) {
            cmbTipoReporte.setValue(R_INCIDENCIAS);
            onGenerarReporte(null);
        }
    }
}
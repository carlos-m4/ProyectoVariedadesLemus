package controller;

import DAO.ReporteDAO;
import DAO.VentaDAO;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Sesion;
import model.Usuario;

import java.time.LocalDate;

public class PanelPrincipalEmpleadoController {

    @FXML private Label lblBienvenidaEmpleado;
    @FXML private Label lblMontoVentasDia;
    @FXML private Label lblCantTransaccionesDia;
    @FXML private Label lblVentasRegistradas;
    @FXML private Label lblVentasJornada;
    @FXML private Label lblIncidenciasCant;
    @FXML private Label lblIncidenciasAtencion;
    @FXML private Label lblVerPanelVentas;
    @FXML private Label lblConsultarIncidencias;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    @FXML
    private void initialize() {
        Usuario usuario = Sesion.getUsuario();
        Integer idUsuario = usuario == null ? null : usuario.getIdUsuario();
        LocalDate hoy = LocalDate.now();

        lblBienvenidaEmpleado.setText("¡Bienvenido/a, " + (usuario == null ? "" : usuario.getNombre()) + "!");

        int ventas = ventaDAO.cantidadDelDia(hoy, idUsuario);
        lblMontoVentasDia.setText(Mensajes.dinero(ventaDAO.totalDelDia(hoy, idUsuario)));
        lblCantTransaccionesDia.setText(ventas + (ventas == 1 ? " transacción" : " transacciones"));
        lblVentasRegistradas.setText(String.valueOf(ventas));


        int pendientes = reporteDAO.contarPendientes();
        lblIncidenciasCant.setText(String.valueOf(pendientes));
        if (pendientes == 0) {
            lblIncidenciasAtencion.setText("Sin pendientes");
            lblIncidenciasAtencion.setTextFill(javafx.scene.paint.Color.web("#0f9c2d"));
        } else {
            lblIncidenciasAtencion.setText("Pendientes de atención");
            lblIncidenciasAtencion.setTextFill(javafx.scene.paint.Color.web("#d01212"));
        }
    }

    @FXML
    void onVentasPP(Event event) {
        PanelPrincipalController.getInstancia().mostrarVentas();
    }

    @FXML
    void onrReportesPP(Event event) {
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        marco.setAccionPendiente(PanelPrincipalController.ACCION_INCIDENCIAS);
        marco.mostrarReportes();
    }

    @FXML
    void onRegistrarIncidenciaPP(ActionEvent event) {
        PanelPrincipalController.getInstancia().abrirSubpanel("/vista/PanelReporteEmpleado.fxml");
    }


    @FXML
    void onExportarIncidenciasPP(ActionEvent event) {
        onrReportesPP(event);
    }
}
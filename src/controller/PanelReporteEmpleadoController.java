package controller;

import DAO.ReporteDAO;
import DAO.VentaDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import model.Reporte;

public class PanelReporteEmpleadoController {
    @FXML private Label lblTituloInc;
    @FXML private TextField txtClienteInc;
    @FXML private ComboBox<String> cmbTipoInc;
    @FXML private TextField txtTicketInc;
    @FXML private TextField txtMontoInc;
    @FXML private TextArea txtDescripcionInc;
    @FXML private Button btnGuardarInc;
    @FXML private Button btnCancelarInc;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    @FXML
    private void initialize() {
        cmbTipoInc.getItems().setAll("Devolución", "Reclamo", "Cambio de producto", "Otro");
        txtTicketInc.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0,9}") ? c : null));
        txtMontoInc.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("[0-9.,$ ]*") ? c : null));
        Platform.runLater(() -> txtClienteInc.requestFocus());
    }

    @FXML void onClienteInc(ActionEvent e) { cmbTipoInc.requestFocus(); }
    @FXML void onTicketInc(ActionEvent e) { txtMontoInc.requestFocus(); }
    @FXML void onMontoInc(ActionEvent e) { txtDescripcionInc.requestFocus(); }

    @FXML
    void onGuardarIncidencia(ActionEvent event) {
        String cliente = txtClienteInc.getText().trim();
        String tipo = cmbTipoInc.getValue();
        String ticket = txtTicketInc.getText().trim();
        String descripcion = txtDescripcionInc.getText().trim();

        if (cliente.isEmpty()) {
            Mensajes.advertencia("Datos incompletos", "Escribe el nombre del cliente.");
            return;
        }
        if (cliente.length() > 100) {
            Mensajes.advertencia("Datos incorrectos", "El nombre del cliente puede tener máximo 100 letras.");
            return;
        }
        if (tipo == null) {
            Mensajes.advertencia("Datos incompletos", "Escoge el tipo de incidencia.");
            return;
        }
        if (descripcion.isEmpty()) {
            Mensajes.advertencia("Datos incompletos", "Describe qué pasó.");
            return;
        }
        if (descripcion.length() > 255) {
            Mensajes.advertencia("Datos incorrectos",
                    "La descripción es muy larga (máximo 255 letras, llevas " + descripcion.length() + ").");
            return;
        }

        double monto;
        try {
            monto = SubPanelINventarioController.leerPrecio(txtMontoInc.getText(), "monto");
        } catch (NumberFormatException e) {
            Mensajes.advertencia("Datos incorrectos", e.getMessage());
            return;
        }

        Integer idVenta = null;
        if (!ticket.isEmpty()) {
            idVenta = Integer.parseInt(ticket);
            if (!ventaDAO.existe(idVenta)) {
                Mensajes.advertencia("Ticket no encontrado",
                        "No existe una venta con el número " + ticket + ". Revisa el ticket o deja la casilla vacía.");
                return;
            }
        }

        Reporte r = new Reporte();
        r.setCliente(cliente);
        r.setTipo(tipo);
        r.setDescripcion(descripcion);
        r.setMonto(monto);
        r.setIdVenta(idVenta);

        if (!reporteDAO.insertar(r)) {
            Mensajes.error("Error", "No se pudo guardar la incidencia. Revisa la conexión con la base de datos.");
            return;
        }

        Mensajes.info("Incidencia registrada", "La incidencia de \"" + cliente + "\" quedó como Pendiente.");
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        marco.cerrarSubpanel();
        marco.mostrarInicio();
    }

    @FXML
    void onCancelarIncidencia(ActionEvent event) {
        PanelPrincipalController.getInstancia().cerrarSubpanel();
    }
}

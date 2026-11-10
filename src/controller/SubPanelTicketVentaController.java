package controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.DetalleVenta;
import model.Venta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SubPanelTicketVentaController {
    @FXML private VBox tarjetaTicket;
    @FXML private Label lblTicketNum;
    @FXML private Label lblFecha;
    @FXML private Label lblAtendio;
    @FXML private TableView<DetalleVenta> tblTicketDetalle;
    @FXML private TableColumn<DetalleVenta, Integer> colCant;
    @FXML private TableColumn<DetalleVenta, String> colNombre;
    @FXML private TableColumn<DetalleVenta, Double> colPrecio;

    @FXML private Label lblTotalTicket;
    @FXML private Label lblTotalCobrado;
    @FXML private Label lblMetodoPago;
    @FXML private Label lblCantProductos;
    @FXML private Label lblCambio;

    @FXML private Button btnImprimir;
    @FXML private Button btnNuevaVenta;

    @FXML
    private void initialize() {
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Mensajes.dinero(valor));
            }
        });
    }

    public void mostrarVenta(Venta venta) {
        lblTicketNum.setText(String.format("%06d", venta.getIdVenta()));
        lblFecha.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblAtendio.setText(venta.getNombreUsuario());

        tblTicketDetalle.setItems(FXCollections.observableArrayList(venta.getDetalles()));

        String total = Mensajes.dinero(venta.getTotal()).replace("$", "$ ");
        lblTotalTicket.setText(total);
        lblTotalCobrado.setText(total);
        lblMetodoPago.setText(venta.getMetodoPago());
        lblCantProductos.setText(String.valueOf(venta.getCantidadProductos()));
        lblCambio.setText(Mensajes.dinero(venta.getCambio()).replace("$", "$ "));
    }

    @FXML
    private void onImprimirTicket(ActionEvent event) {
        Mensajes.info("Imprimir ticket", "El ticket N.º " + lblTicketNum.getText() + " se imprimió correctamente.");
    }

    @FXML
    private void onNuevaVenta(ActionEvent event) {
        Stage ventana = (Stage) btnNuevaVenta.getScene().getWindow();
        ventana.close();
    }
}
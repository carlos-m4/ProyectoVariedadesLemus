package controller;

import DAO.ProductoDAO;
import DAO.VentaDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.DetalleVenta;
import model.Producto;
import model.Sesion;
import model.Usuario;
import model.Venta;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class RegistroVentaController {
    private static final String EFECTIVO = "Efectivo";
    private static final String TARJETA = "Tarjeta";

    @FXML private TextField txtSeach_GV;
    @FXML private TableView<Producto> tblProductos_GV;
    @FXML private TableColumn<Producto, Integer> ColId_GV;
    @FXML private TableColumn<Producto, String> ColName_GV;
    @FXML private TableColumn<Producto, Double> ColPrecio_GV;
    @FXML private TableColumn<Producto, Producto> Action_GV;

    @FXML private TableView<DetalleVenta> tblCarrito_GV;
    @FXML private TableColumn<DetalleVenta, String> Name_compra_GV;
    @FXML private TableColumn<DetalleVenta, Double> Precio_GV;
    @FXML private TableColumn<DetalleVenta, Integer> Cant_GV;
    @FXML private Label TotalsinIBA_GV;
    @FXML private Label TotalIBA_GV;
    @FXML private Label TotalFinal_GV;
    @FXML private Label money_GV;
    @FXML private Label MoneyDigital_GV;
    @FXML private Button AndVenta_GV;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> filtrados;

    private final ObservableList<DetalleVenta> carrito = FXCollections.observableArrayList();

    private String metodoPago = EFECTIVO;

    @FXML
    private void initialize() {
        configurarTablaProductos();
        configurarCarrito();

        txtSeach_GV.textProperty().addListener((obs, antes, ahora) -> filtrar());

        elegirMetodo(EFECTIVO);
        cargarProductos();
        actualizarTotales();

        Platform.runLater(() -> txtSeach_GV.requestFocus());
    }

    @FXML
    void onBuscarProducto(KeyEvent event) {
        filtrar();
    }

    @FXML
    void onBuscar(ActionEvent event) {
        String texto = txtSeach_GV.getText().trim();
        if (texto.isEmpty()) {
            return;
        }

        Producto elegido = null;

        if (texto.matches("\\d+")) {
            int codigo = Integer.parseInt(texto);
            for (Producto p : productos) {
                if (p.getIdProducto() == codigo) {
                    elegido = p;
                    break;
                }
            }
        }

        if (elegido == null && filtrados.size() == 1) {
            elegido = filtrados.get(0);
        }

        if (elegido != null) {
            agregarAlCarrito(elegido);
            txtSeach_GV.clear();
        }
    }

    @FXML
    void OnclickEfectivoGV(MouseEvent event) {
        elegirMetodo(EFECTIVO);
    }

    @FXML
    void OnclickTarjetaGV(MouseEvent event) {
        elegirMetodo(TARJETA);
    }

    @FXML
    void onAgregarProducto(ActionEvent event) {
        if (carrito.isEmpty()) {
            Mensajes.advertencia("Venta", "El carrito está vacío. Agrega al menos un producto.");
            return;
        }

        Usuario usuario = Sesion.getUsuario();
        if (usuario == null) {
            Mensajes.error("Venta", "No hay una sesión activa. Vuelve a iniciar sesión.");
            return;
        }

        Venta venta = new Venta();
        venta.setIdUsuario(usuario.getIdUsuario());
        venta.setNombreUsuario(usuario.getNombreCompleto());
        venta.setMetodoPago(metodoPago);
        for (DetalleVenta d : carrito) {
            venta.getDetalles().add(new DetalleVenta(d.getIdProducto(), d.getNombreProducto(),
                    d.getPrecioUnitario(), d.getCantidad()));
        }
        double total = venta.calcularTotal();

        if (EFECTIVO.equals(metodoPago)) {
            Double recibido = pedirEfectivo(total);
            if (recibido == null) {
                return;
            }
            venta.setEfectivoRecibido(recibido);
            venta.setCambio(Venta.redondear(recibido - total));
        } else {
            venta.setEfectivoRecibido(total);
            venta.setCambio(0);
        }

        try {
            ventaDAO.registrarVenta(venta);
        } catch (SQLException e) {
            Mensajes.error("No se pudo completar la venta", Mensajes.causaReal(e));
            cargarProductos();
            return;
        }

        mostrarTicket(venta);
        limpiarVenta();
    }

    private void configurarTablaProductos() {
        ColId_GV.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        ColName_GV.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        ColPrecio_GV.setCellValueFactory(new PropertyValueFactory<>("precio"));
        ColPrecio_GV.setCellFactory(c -> celdaDinero());

        Action_GV.setCellValueFactory(dato -> new SimpleObjectProperty<>(dato.getValue()));
        Action_GV.setCellFactory(c -> new TableCell<>() {
            private final Button boton = new Button("Agregar");
            {
                boton.setStyle("-fx-background-color: rgba(15, 156, 45, 1); -fx-text-fill: white;"
                        + " -fx-background-radius: 5; -fx-font-size: 11px; -fx-padding: 2 10;");
                boton.setOnAction(e -> {
                    Producto p = getItem();
                    if (p != null) {
                        agregarAlCarrito(p);
                    }
                });
            }

            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }

                boolean agotado = p.getStock() <= 0;
                boton.setText(agotado ? "Agotado" : "Agregar");
                boton.setDisable(agotado);
                setGraphic(boton);
            }
        });

        tblProductos_GV.setRowFactory(t -> {
            TableRow<Producto> fila = new TableRow<>() {
                @Override
                protected void updateItem(Producto p, boolean vacio) {
                    super.updateItem(p, vacio);
                    setTooltip(vacio || p == null ? null : new Tooltip("Quedan " + p.getStock() + " en bodega"));
                }
            };

            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    agregarAlCarrito(fila.getItem());
                }
            });
            return fila;
        });

        filtrados = new FilteredList<>(productos, p -> true);
        tblProductos_GV.setItems(filtrados);
        tblProductos_GV.setPlaceholder(new Label("No se encontraron productos"));
    }

    private void configurarCarrito() {
        Name_compra_GV.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        Precio_GV.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        Precio_GV.setCellFactory(c -> celdaDineroCarrito());
        Cant_GV.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        tblCarrito_GV.setItems(carrito);
        tblCarrito_GV.setPlaceholder(new Label("Carrito vacío"));

        MenuItem sumar = new MenuItem("Sumar uno");
        MenuItem restar = new MenuItem("Restar uno");
        MenuItem cambiar = new MenuItem("Cambiar cantidad...");
        MenuItem quitar = new MenuItem("Quitar del carrito");
        sumar.setOnAction(e -> cambiarCantidad(seleccionCarrito(), +1));
        restar.setOnAction(e -> cambiarCantidad(seleccionCarrito(), -1));
        cambiar.setOnAction(e -> pedirCantidad(seleccionCarrito()));
        quitar.setOnAction(e -> quitarDelCarrito(seleccionCarrito()));
        tblCarrito_GV.setContextMenu(new ContextMenu(sumar, restar, cambiar, quitar));

        tblCarrito_GV.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                pedirCantidad(seleccionCarrito());
            }
        });

        tblCarrito_GV.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                quitarDelCarrito(seleccionCarrito());
            }
        });

        Tooltip.install(tblCarrito_GV,
                new Tooltip("Doble clic: cambiar cantidad · Clic derecho: más opciones · Supr: quitar"));
    }

    private void cargarProductos() {
        productos.setAll(productoDAO.listar(true));
        filtrar();
    }

    private void filtrar() {
        String texto = txtSeach_GV.getText() == null ? "" : txtSeach_GV.getText().trim().toLowerCase();
        filtrados.setPredicate(p -> texto.isEmpty()
                || p.getNombre().toLowerCase().contains(texto)
                || String.valueOf(p.getIdProducto()).equals(texto));
    }

    private void agregarAlCarrito(Producto p) {
        DetalleVenta linea = buscarEnCarrito(p.getIdProducto());
        int yaLleva = linea == null ? 0 : linea.getCantidad();

        if (yaLleva + 1 > p.getStock()) {
            Mensajes.advertencia("Sin existencias",
                    "Solo quedan " + p.getStock() + " de \"" + p.getNombre() + "\".");
            return;
        }

        if (linea == null) {
            linea = new DetalleVenta(p.getIdProducto(), p.getNombre(), p.getPrecio(), 1);
            carrito.add(linea);
        } else {
            linea.sumarUno();
        }
        tblCarrito_GV.refresh();
        tblCarrito_GV.getSelectionModel().select(linea);
        actualizarTotales();
    }

    private void cambiarCantidad(DetalleVenta linea, int cuanto) {
        if (linea == null) {
            return;
        }
        ponerCantidad(linea, linea.getCantidad() + cuanto);
    }

    private void pedirCantidad(DetalleVenta linea) {
        if (linea == null) {
            return;
        }
        TextInputDialog dialogo = new TextInputDialog(String.valueOf(linea.getCantidad()));
        dialogo.setTitle("Cambiar cantidad");
        dialogo.setHeaderText(linea.getNombreProducto());
        dialogo.setContentText("Cantidad:");

        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) {
            return;
        }
        try {
            ponerCantidad(linea, Integer.parseInt(respuesta.get().trim()));
        } catch (NumberFormatException e) {
            Mensajes.advertencia("Cantidad", "Escribe un número entero.");
        }
    }

    private void ponerCantidad(DetalleVenta linea, int nueva) {
        if (nueva <= 0) {
            quitarDelCarrito(linea);
            return;
        }
        Producto p = buscarProducto(linea.getIdProducto());
        int disponible = p == null ? 0 : p.getStock();
        if (nueva > disponible) {
            Mensajes.advertencia("Sin existencias",
                    "Solo quedan " + disponible + " de \"" + linea.getNombreProducto() + "\".");
            return;
        }
        linea.setCantidad(nueva);
        tblCarrito_GV.refresh();
        actualizarTotales();
    }

    private void quitarDelCarrito(DetalleVenta linea) {
        if (linea != null) {
            carrito.remove(linea);
            actualizarTotales();
        }
    }

    private void actualizarTotales() {
        Venta temporal = new Venta();
        temporal.getDetalles().addAll(carrito);
        double total = temporal.calcularTotal();

        TotalsinIBA_GV.setText(numero(temporal.getSubtotal()));
        TotalIBA_GV.setText(numero(temporal.getMontoIva()));
        TotalFinal_GV.setText(numero(total));
        AndVenta_GV.setDisable(carrito.isEmpty());
    }

    private void elegirMetodo(String metodo) {
        metodoPago = metodo;
        money_GV.getStyleClass().remove("seleccionado");
        MoneyDigital_GV.getStyleClass().remove("seleccionado");
        if (EFECTIVO.equals(metodo)) {
            money_GV.getStyleClass().add("seleccionado");
        } else {
            MoneyDigital_GV.getStyleClass().add("seleccionado");
        }
    }

    private Double pedirEfectivo(double total) {
        while (true) {
            TextInputDialog dialogo = new TextInputDialog(numero(total));
            dialogo.setTitle("Pago en efectivo");
            dialogo.setHeaderText("Total a cobrar: " + Mensajes.dinero(total));
            dialogo.setContentText("¿Con cuánto paga el cliente? $");

            Optional<String> respuesta = dialogo.showAndWait();
            if (respuesta.isEmpty()) {
                return null;
            }
            try {
                double recibido = SubPanelINventarioController.leerPrecio(respuesta.get(), "efectivo");
                if (recibido + 0.001 < total) {
                    Mensajes.advertencia("Pago incompleto",
                            "El cliente debe pagar al menos " + Mensajes.dinero(total) + ".");
                    continue;
                }
                return recibido;
            } catch (NumberFormatException e) {
                Mensajes.advertencia("Pago", "Escribe una cantidad válida, por ejemplo 20.00");
            }
        }
    }

    private void mostrarTicket(Venta venta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista/SubPanelVentaTicket.fxml"));
            Parent vista = loader.load();
            SubPanelTicketVentaController ticket = loader.getController();
            ticket.mostrarVenta(venta);

            Stage ventana = new Stage();
            ventana.setTitle("Ticket de venta N.º " + venta.getIdVenta());
            ventana.initOwner(tblCarrito_GV.getScene().getWindow());
            ventana.initModality(Modality.WINDOW_MODAL);
            ventana.setResizable(false);
            ventana.setScene(new Scene(vista));
            ventana.showAndWait();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();

            Mensajes.info("Venta registrada", "Venta N.º " + venta.getIdVenta() + " guardada por "
                    + Mensajes.dinero(venta.getTotal()) + ".\nCambio: " + Mensajes.dinero(venta.getCambio()));
        }
    }

    private void limpiarVenta() {
        carrito.clear();
        txtSeach_GV.clear();
        elegirMetodo(EFECTIVO);
        cargarProductos();
        actualizarTotales();
        txtSeach_GV.requestFocus();
    }

    private DetalleVenta buscarEnCarrito(int idProducto) {
        for (DetalleVenta d : carrito) {
            if (d.getIdProducto() == idProducto) {
                return d;
            }
        }
        return null;
    }

    private Producto buscarProducto(int idProducto) {
        for (Producto p : productos) {
            if (p.getIdProducto() == idProducto) {
                return p;
            }
        }
        return null;
    }

    private DetalleVenta seleccionCarrito() {
        return tblCarrito_GV.getSelectionModel().getSelectedItem();
    }

    private String numero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private TableCell<Producto, Double> celdaDinero() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Mensajes.dinero(valor));
            }
        };
    }

    private TableCell<DetalleVenta, Double> celdaDineroCarrito() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Mensajes.dinero(valor));
            }
        };
    }
}
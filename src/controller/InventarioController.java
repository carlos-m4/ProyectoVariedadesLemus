package controller;

import DAO.ProductoDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.FileChooser;
import model.Producto;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class InventarioController {


    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, Double> colPrecio_Compra;
    @FXML private TableColumn<Producto, Double> colPrecio_Venta;
    @FXML private TableColumn<Producto, Integer> col_Stock;
    @FXML private TableColumn<Producto, String> col_estado;
    @FXML private ImageView btnEditar;
    @FXML private ImageView btnEliminar;


    private static final String TODAS = "Todas";

    private final ProductoDAO productoDAO = new ProductoDAO();


    private final ObservableList<Producto> todos = FXCollections.observableArrayList();


    private FilteredList<Producto> filtrados;


    @FXML
    private void initialize() {
        configurarColumnas();
        configurarFilas();

        filtrados = new FilteredList<>(todos, p -> true);
        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblProductos.comparatorProperty());
        tblProductos.setItems(ordenados);
        tblProductos.setPlaceholder(new Label("No hay productos para mostrar"));


        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());


        Tooltip.install(btnEditar, new Tooltip("Editar el producto seleccionado"));
        Tooltip.install(btnEliminar, new Tooltip("Eliminar o restaurar el producto seleccionado"));

        recargar();

        Platform.runLater(this::revisarAccionPendiente);
    }

    @FXML
    void onAgregarProducto(ActionEvent event) {
        abrirFormulario(null);
    }

    @FXML
    void onBuscar(ActionEvent event) {
        filtrar();
    }

    @FXML
    void onFiltrarCategoria(ActionEvent event) {
        filtrar();
    }

    @FXML
    void onTablaClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Producto elegido = tblProductos.getSelectionModel().getSelectedItem();
            if (elegido != null) {
                abrirFormulario(elegido);
            }
        }
    }

    @FXML
    void onEditarProducto(MouseEvent event) {
        Producto elegido = elegido();
        if (elegido != null) {
            abrirFormulario(elegido);
        }
    }

    @FXML
    void onEliminarProducto(MouseEvent event) {
        Producto p = elegido();
        if (p == null) {
            return;
        }

        if (p.estaActivo()) {
            boolean si = Mensajes.confirmar("Eliminar producto",
                    "¿Seguro que quieres eliminar \"" + p.getNombre() + "\"?\n\n"
                            + "Ya no aparecerá para vender.");
            if (si && productoDAO.cambiarEstado(p.getIdProducto(), "Inactivo")) {
                recargar();
                Mensajes.info("Producto eliminado",
                        "\"" + p.getNombre() + "\" fue eliminado.\n"
                                + "Si fue un error, selecciónalo y toca el basurero otra vez para restaurarlo.");
            }
        } else {
            boolean si = Mensajes.confirmar("Restaurar producto",
                    "\"" + p.getNombre() + "\" está eliminado.\n\n"
                            + "¿Quieres deshacer la eliminación y volver a tenerlo disponible?");
            if (si && productoDAO.cambiarEstado(p.getIdProducto(), "Activo")) {
                recargar();
                Mensajes.info("Producto restaurado", "\"" + p.getNombre() + "\" ya está disponible otra vez.");
            }
        }
    }

    public void abrirFormulario(Producto p) {
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        SubPanelINventarioController formulario = marco.abrirSubpanel("/vista/SubPanelInventario.fxml");
        if (formulario == null) {
            return;
        }
        if (p != null) {
            formulario.cargarProducto(p);
        }
        formulario.setAlGuardar(this::recargarYSeleccionar);
        formulario.setAlImportar(this::importarCSV);
    }

    public void importarCSV() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Importar inventario (CSV)");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));

        File archivo = selector.showOpenDialog(tblProductos.getScene().getWindow());
        if (archivo == null) {
            return; // Cerró la ventanita sin escoger nada.
        }

        ImportadorCSV.Resultado lectura;
        try {
            lectura = ImportadorCSV.leer(archivo);
        } catch (IOException e) {
            Mensajes.error("Importar", "No se pudo leer el archivo.\n" + Mensajes.causaReal(e));
            return;
        }

        if (lectura.productos.isEmpty()) {
            Mensajes.advertencia("Importar",
                    "No se encontró ningún producto válido en el archivo.\n\n" + resumirErrores(lectura.errores));
            return;
        }

        int[] resultado = productoDAO.importar(lectura.productos);
        recargar();

        String texto = "Productos guardados: " + resultado[0]
                + "\nOmitidos (ya existían o venían repetidos): " + resultado[1]
                + "\nLíneas con errores: " + lectura.errores.size();
        if (!lectura.errores.isEmpty()) {
            texto += "\n\n" + resumirErrores(lectura.errores);
        }
        Mensajes.info("Importación terminada", texto);
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio_Compra.setCellValueFactory(new PropertyValueFactory<>("precioCosto"));
        colPrecio_Venta.setCellValueFactory(new PropertyValueFactory<>("precio"));
        col_Stock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        col_estado.setCellValueFactory(dato -> {
            Producto p = dato.getValue();
            String texto;
            if (!p.estaActivo()) {
                texto = "Eliminado";
            } else if (p.isStockBajo()) {
                texto = "Stock bajo";
            } else {
                texto = "Activo";
            }
            return new SimpleStringProperty(texto);
        });


        colPrecio_Compra.setCellFactory(columna -> celdaDinero());
        colPrecio_Venta.setCellFactory(columna -> celdaDinero());
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

    private void configurarFilas() {
        tblProductos.setRowFactory(tabla -> new TableRow<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                getStyleClass().removeAll("fila-stock-bajo", "fila-inactiva");
                if (vacio || p == null) {
                    return;
                }
                if (!p.estaActivo()) {
                    getStyleClass().add("fila-inactiva");
                } else if (p.isStockBajo()) {
                    getStyleClass().add("fila-stock-bajo");
                }
            }
        });
    }


    private void recargar() {
        todos.setAll(productoDAO.listar(false));

        String elegida = cmbCategoria.getValue();
        List<String> categorias = productoDAO.listarCategorias();
        categorias.add(0, TODAS);
        cmbCategoria.getItems().setAll(categorias);

        if (elegida != null && categorias.contains(elegida)) {
            cmbCategoria.setValue(elegida);
        } else {
            cmbCategoria.setValue(null);
        }
        filtrar();
    }

    private void recargarYSeleccionar(int idProducto) {
        recargar();
        for (Producto p : tblProductos.getItems()) {
            if (p.getIdProducto() == idProducto) {
                tblProductos.getSelectionModel().select(p);
                tblProductos.scrollTo(p);
                break;
            }
        }
    }

    private void filtrar() {
        if (filtrados == null) {
            return;
        }
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String categoria = cmbCategoria.getValue();
        boolean todasCategorias = categoria == null || categoria.equals(TODAS);

        filtrados.setPredicate(p -> {
            boolean coincideTexto = texto.isEmpty()
                    || p.getNombre().toLowerCase().contains(texto)
                    || String.valueOf(p.getIdProducto()).equals(texto);
            boolean coincideCategoria = todasCategorias || categoria.equalsIgnoreCase(p.getCategoria());
            return coincideTexto && coincideCategoria;
        });
    }

    private Producto elegido() {
        Producto p = tblProductos.getSelectionModel().getSelectedItem();
        if (p == null) {
            Mensajes.advertencia("Inventario", "Primero selecciona un producto de la tabla.");
        }
        return p;
    }

    private void revisarAccionPendiente() {
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        if (marco == null) {
            return;
        }
        String accion = marco.tomarAccionPendiente();
        if (PanelPrincipalController.ACCION_NUEVO.equals(accion)) {
            abrirFormulario(null);
        } else if (PanelPrincipalController.ACCION_IMPORTAR.equals(accion)) {
            importarCSV();
        }
    }

    private String resumirErrores(List<String> errores) {
        StringBuilder sb = new StringBuilder();
        int limite = Math.min(errores.size(), 8);
        for (int i = 0; i < limite; i++) {
            sb.append("• ").append(errores.get(i)).append("\n");
        }
        if (errores.size() > limite) {
            sb.append("... y ").append(errores.size() - limite).append(" más.");
        }
        return sb.toString();
    }
}
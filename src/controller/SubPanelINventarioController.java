package controller;

import DAO.ProductoDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import model.Producto;

import java.util.function.IntConsumer;

public class SubPanelINventarioController {


    @FXML private Label lblTituloProducto;
    @FXML private TextField txtcodigo_produc;
    @FXML private TextField txtcodigo_produc1;
    @FXML private TextField txtcodigo_produc11;
    @FXML private TextField DescripProductADDP;
    @FXML private TextField PrecioComADDP;
    @FXML private TextField btnprecio_product_venta;
    @FXML private TextField StockADDP;
    @FXML private TextField StockMInADDP;
    @FXML private Button btnGuardar_product;
    @FXML private Button btnCancel_product;
    @FXML private Button btn_import_product;

    private final ProductoDAO productoDAO = new ProductoDAO();

    private Producto original;

    private IntConsumer alGuardar;
    private Runnable alImportar;


    @FXML
    private void initialize() {

        PrecioComADDP.setTextFormatter(soloPrecio());
        btnprecio_product_venta.setTextFormatter(soloPrecio());

        StockADDP.setTextFormatter(soloEnteros());
        StockMInADDP.setTextFormatter(soloEnteros());

        Platform.runLater(() -> txtcodigo_produc1.requestFocus());
    }


    // Llena el formulario con un producto para editarlo.
    public void cargarProducto(Producto p) {
        original = p;
        lblTituloProducto.setText("Editar producto");

        // Al editar no se importa, así que escondemos ese botón.
        btn_import_product.setVisible(false);

        txtcodigo_produc.setText(String.valueOf(p.getIdProducto()));
        txtcodigo_produc1.setText(p.getNombre());
        txtcodigo_produc11.setText(p.getCategoria());
        DescripProductADDP.setText(p.getDescripcion() == null ? "" : p.getDescripcion());
        PrecioComADDP.setText(String.format(java.util.Locale.US, "%.2f", p.getPrecioCosto()));
        btnprecio_product_venta.setText(String.format(java.util.Locale.US, "%.2f", p.getPrecio()));
        StockADDP.setText(String.valueOf(p.getStock()));
        StockMInADDP.setText(String.valueOf(p.getStockMinimo()));
    }

    public void setAlGuardar(IntConsumer alGuardar) {
        this.alGuardar = alGuardar;
    }

    public void setAlImportar(Runnable alImportar) {
        this.alImportar = alImportar;
    }

    @FXML void onCod(ActionEvent e) { txtcodigo_produc1.requestFocus(); }
    @FXML void onName_product(ActionEvent e) { txtcodigo_produc11.requestFocus(); }
    @FXML void on_categ_producto(ActionEvent e) { DescripProductADDP.requestFocus(); }
    @FXML void on_descripcion_product(ActionEvent e) { PrecioComADDP.requestFocus(); }
    @FXML void on_precio_product(ActionEvent e) { btnprecio_product_venta.requestFocus(); }
    @FXML void on_precio_product_venta(ActionEvent e) { StockADDP.requestFocus(); }
    @FXML void on_stock(ActionEvent e) { StockMInADDP.requestFocus(); }
    @FXML void on_stock_minimo(ActionEvent e) { btnGuardar_product.requestFocus(); }

    @FXML
    void onsave_product(ActionEvent event) {
        Producto p;
        try {
            p = leerFormulario();
        } catch (NumberFormatException ex) {
            Mensajes.advertencia("Datos incorrectos", ex.getMessage());
            return;
        }

        String problema = validar(p);
        if (problema != null) {
            Mensajes.advertencia("Datos incorrectos", problema);
            return;
        }

        boolean cambioNombre = original == null || !original.getNombre().equalsIgnoreCase(p.getNombre());
        if (cambioNombre && productoDAO.existeNombre(p.getNombre())) {
            Mensajes.advertencia("Producto repetido", "Ya existe un producto llamado \"" + p.getNombre() + "\".");
            return;
        }

        if (p.getPrecio() < p.getPrecioCosto()) {
            boolean seguir = Mensajes.confirmar("Precio bajo",
                    "El precio de venta es menor que el de compra.\n¿Seguro que quieres guardarlo así?");
            if (!seguir) {
                return;
            }
        }

        int idGuardado;
        if (original == null) {
            idGuardado = productoDAO.insertar(p);
            if (idGuardado < 0) {
                Mensajes.error("Error", "No se pudo guardar el producto. Revisa la conexión con la base de datos.");
                return;
            }
            Mensajes.info("Producto guardado", "Se guardó \"" + p.getNombre() + "\" con el código " + idGuardado + ".");
        } else {
            p.setIdProducto(original.getIdProducto());
            p.setEstado(original.getEstado());
            if (!productoDAO.actualizar(p)) {
                Mensajes.error("Error", "No se pudieron guardar los cambios.");
                return;
            }
            idGuardado = p.getIdProducto();
        }

        PanelPrincipalController.getInstancia().cerrarSubpanel();
        if (alGuardar != null) {
            alGuardar.accept(idGuardado);
        }
    }

    @FXML
    void onCancelaccion(ActionEvent event) {
        PanelPrincipalController.getInstancia().cerrarSubpanel();
    }

    @FXML
    void onImportar(ActionEvent event) {
        PanelPrincipalController.getInstancia().cerrarSubpanel();
        if (alImportar != null) {
            alImportar.run();
        }
    }

    public static String validar(Producto p) {
        String nombre = p.getNombre() == null ? "" : p.getNombre().trim();
        String categoria = p.getCategoria() == null ? "" : p.getCategoria().trim();
        String descripcion = p.getDescripcion() == null ? "" : p.getDescripcion().trim();

        if (nombre.isEmpty()) {
            return "Escribe el nombre del producto.";
        }
        if (nombre.length() > 100) {
            return "El nombre es muy largo (máximo 100 letras).";
        }
        if (categoria.isEmpty()) {
            return "Escribe la categoría del producto.";
        }
        if (categoria.length() > 50) {
            return "La categoría es muy larga (máximo 50 letras).";
        }
        if (descripcion.length() > 255) {
            return "La descripción es muy larga (máximo 255 letras).";
        }
        if (p.getPrecioCosto() < 0) {
            return "El precio de compra no puede ser negativo.";
        }
        if (p.getPrecio() <= 0) {
            return "El precio de venta debe ser mayor que $0.00.";
        }
        if (p.getPrecio() > 99_999_999 || p.getPrecioCosto() > 99_999_999) {
            return "El precio es demasiado grande.";
        }
        if (p.getStock() < 0 || p.getStockMinimo() < 0) {
            return "El stock no puede ser negativo.";
        }
        return null;
    }

    public static double leerPrecio(String texto, String campo) {
        String limpio = texto == null ? "" : texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) {
            return 0;
        }

        if (limpio.contains(",") && !limpio.contains(".")) {
            limpio = limpio.replace(",", ".");
        } else {
            limpio = limpio.replace(",", "");
        }
        try {
            double valor = Double.parseDouble(limpio);
            return Math.round(valor * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El " + campo + " no es un número válido: \"" + texto + "\"");
        }
    }

    public static int leerEntero(String texto, String campo) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("El " + campo + " debe ser un número entero: \"" + texto + "\"");
        }
    }

    private Producto leerFormulario() {
        Producto p = new Producto();
        p.setNombre(txtcodigo_produc1.getText().trim());
        p.setCategoria(txtcodigo_produc11.getText().trim());
        p.setDescripcion(DescripProductADDP.getText().trim());
        p.setPrecioCosto(leerPrecio(PrecioComADDP.getText(), "precio de compra"));
        p.setPrecio(leerPrecio(btnprecio_product_venta.getText(), "precio de venta"));
        p.setStock(leerEntero(StockADDP.getText(), "stock"));
        p.setStockMinimo(leerEntero(StockMInADDP.getText(), "stock mínimo"));
        return p;
    }

    private TextFormatter<String> soloPrecio() {
        return new TextFormatter<>(cambio ->
                cambio.getControlNewText().matches("[0-9.,$ ]*") ? cambio : null);
    }

    private TextFormatter<String> soloEnteros() {
        return new TextFormatter<>(cambio ->
                cambio.getControlNewText().matches("\\d{0,9}") ? cambio : null);
    }
}
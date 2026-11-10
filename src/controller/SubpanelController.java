package controller;

import DAO.UsuarioDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import model.Sesion;
import model.Usuario;

public class SubpanelController {
    private static final String TODOS = "Todos";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbRolUser;
    @FXML private Button btnNewusuario;
    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colNombreUser;
    @FXML private TableColumn<Usuario, String> colApellidoUser;
    @FXML private TableColumn<Usuario, String> colUsuarioUser;
    @FXML private TableColumn<Usuario, String> colRolUser;
    @FXML private TableColumn<Usuario, String> colEstadoUser;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ObservableList<Usuario> todos = FXCollections.observableArrayList();
    private FilteredList<Usuario> filtrados;

    @FXML
    private void initialize() {
        colNombreUser.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidoUser.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colUsuarioUser.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colRolUser.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstadoUser.setCellValueFactory(dato ->
                new SimpleStringProperty(dato.getValue().estaActivo() ? "Activo" : "Eliminado"));

        tblUsuarios.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                getStyleClass().remove("fila-inactiva");
                if (!vacio && u != null && !u.estaActivo()) {
                    getStyleClass().add("fila-inactiva");
                }
            }
        });

        filtrados = new FilteredList<>(todos, u -> true);
        SortedList<Usuario> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblUsuarios.comparatorProperty());
        tblUsuarios.setItems(ordenados);
        tblUsuarios.setPlaceholder(new Label("No hay usuarios para mostrar"));

        cmbRolUser.getItems().setAll(TODOS, "Administrador", "Empleado");
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());

        configurarAtajos();
        recargar();

        Platform.runLater(this::revisarAccionPendiente);
    }

    @FXML
    void onBuscar(ActionEvent event) {
        filtrar();
    }

    @FXML
    void onFiltrarRol(ActionEvent event) {
        filtrar();
    }

    @FXML
    void onNewUsuarios(ActionEvent event) {
        abrirFormulario(null);
    }

    public void abrirFormulario(Usuario usuario) {
        SubPanelAddUsuariosController formulario =
                PanelPrincipalController.getInstancia().abrirSubpanel("/vista/SubPanelGestionUsuarios.fxml");
        if (formulario == null) {
            return;
        }
        if (usuario != null) {
            formulario.cargarUsuario(usuario);
        }
        formulario.setAlGuardar(this::recargarYSeleccionar);
    }

    private void configurarAtajos() {
        MenuItem editar = new MenuItem("Editar usuario");
        MenuItem eliminar = new MenuItem("Eliminar / restaurar");
        editar.setOnAction(e -> editarSeleccionado());
        eliminar.setOnAction(e -> eliminarORestaurar());
        tblUsuarios.setContextMenu(new ContextMenu(editar, eliminar));

        tblUsuarios.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                editarSeleccionado();
            }
        });

        tblUsuarios.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                eliminarORestaurar();
            } else if (e.getCode() == KeyCode.ENTER) {
                editarSeleccionado();
            }
        });

        Tooltip.install(tblUsuarios,
                new Tooltip("Doble clic: editar · Clic derecho: más opciones · Supr: eliminar o restaurar"));
    }

    private void editarSeleccionado() {
        Usuario u = seleccionado();
        if (u != null) {
            abrirFormulario(u);
        }
    }

    private void eliminarORestaurar() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }

        if (u.estaActivo()) {
            Usuario actual = Sesion.getUsuario();
            if (actual != null && actual.getIdUsuario() == u.getIdUsuario()) {
                Mensajes.advertencia("Eliminar usuario", "No puedes eliminar tu propio usuario mientras lo estás usando.");
                return;
            }
            if (u.esAdministrador() && contarAdministradoresActivos() <= 1) {
                Mensajes.advertencia("Eliminar usuario", "Debe quedar al menos un administrador activo en el sistema.");
                return;
            }
            boolean si = Mensajes.confirmar("Eliminar usuario",
                    "¿Seguro que quieres eliminar a \"" + u.getNombreCompleto() + "\"?\n\n"
                            + "Ya no podrá iniciar sesión.");
            if (si && usuarioDAO.cambiarEstado(u.getIdUsuario(), "Inactivo")) {
                recargar();
                Mensajes.info("Usuario eliminado",
                        "\"" + u.getNombreCompleto() + "\" fue eliminado.\n"
                                + "Si fue un error, selecciónalo y elige \"Eliminar / restaurar\" otra vez para restaurarlo.");
            }
        } else {
            boolean si = Mensajes.confirmar("Restaurar usuario",
                    "\"" + u.getNombreCompleto() + "\" está eliminado.\n\n"
                            + "¿Quieres deshacer la eliminación y que pueda volver a entrar?");
            if (si && usuarioDAO.cambiarEstado(u.getIdUsuario(), "Activo")) {
                recargar();
                Mensajes.info("Usuario restaurado", "\"" + u.getNombreCompleto() + "\" ya puede iniciar sesión otra vez.");
            }
        }
    }

    private int contarAdministradoresActivos() {
        int cuenta = 0;
        for (Usuario u : todos) {
            if (u.esAdministrador() && u.estaActivo()) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private void recargar() {
        todos.setAll(usuarioDAO.listar());
        filtrar();
    }

    private void recargarYSeleccionar(String nombreUsuario) {
        recargar();
        for (Usuario u : tblUsuarios.getItems()) {
            if (u.getNombreUsuario().equalsIgnoreCase(nombreUsuario)) {
                tblUsuarios.getSelectionModel().select(u);
                tblUsuarios.scrollTo(u);
                break;
            }
        }
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String rol = cmbRolUser.getValue();
        boolean todosLosRoles = rol == null || rol.equals(TODOS);

        filtrados.setPredicate(u -> {
            boolean coincideTexto = texto.isEmpty()
                    || contiene(u.getNombre(), texto)
                    || contiene(u.getApellido(), texto)
                    || contiene(u.getNombreUsuario(), texto);
            boolean coincideRol = todosLosRoles || rol.equalsIgnoreCase(u.getRol());
            return coincideTexto && coincideRol;
        });
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    private Usuario seleccionado() {
        Usuario u = tblUsuarios.getSelectionModel().getSelectedItem();
        if (u == null) {
            Mensajes.advertencia("Usuarios", "Primero selecciona un usuario de la tabla.");
        }
        return u;
    }

    private void revisarAccionPendiente() {
        PanelPrincipalController marco = PanelPrincipalController.getInstancia();
        if (marco != null && PanelPrincipalController.ACCION_NUEVO.equals(marco.tomarAccionPendiente())) {
            abrirFormulario(null);
        }
    }
}
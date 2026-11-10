package controller;

import DAO.UsuarioDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.Sesion;
import model.Usuario;

import java.util.function.Consumer;

public class SubPanelAddUsuariosController {
    private static final String ADMIN = "Administrador";
    private static final String EMPLEADO = "Empleado";

    @FXML private Label lblTituloUsuario;
    @FXML private TextField txtNameUsuario;
    @FXML private TextField txtNameApellido;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtContra;
    @FXML private ComboBox<String> cmbCategoria1;
    @FXML private Button btnCreateUsuario;
    @FXML private Button btnCancel;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario original;
    private Consumer<String> alGuardar;

    @FXML
    private void initialize() {
        cmbCategoria1.getItems().setAll(ADMIN, EMPLEADO);
        Platform.runLater(() -> txtNameUsuario.requestFocus());
    }

    public void cargarUsuario(Usuario u) {
        original = u;
        lblTituloUsuario.setText("Editar usuario");
        btnCreateUsuario.setText("Guardar");

        txtNameUsuario.setText(u.getNombre());
        txtNameApellido.setText(u.getApellido());
        txtUser.setText(u.getNombreUsuario());
        txtContra.setPromptText("Déjala vacía para no cambiarla");
        cmbCategoria1.setValue(u.getRol());
    }

    public void setAlGuardar(Consumer<String> alGuardar) {
        this.alGuardar = alGuardar;
    }

    @FXML void OnNameUSer(ActionEvent e) { txtNameApellido.requestFocus(); }
    @FXML void OnApellidoUSer(ActionEvent e) { txtUser.requestFocus(); }
    @FXML void On_Name_usuario(ActionEvent e) { txtContra.requestFocus(); }
    @FXML void On_Password_usuario(ActionEvent e) { cmbCategoria1.requestFocus(); }
    @FXML void onFiltrarrool(ActionEvent e) { btnCreateUsuario.requestFocus(); }

    @FXML
    void onCrearUSerClick(ActionEvent event) {
        String nombre = txtNameUsuario.getText().trim();
        String apellido = txtNameApellido.getText().trim();
        String usuario = txtUser.getText().trim();
        String clave = txtContra.getText();
        String rol = cmbCategoria1.getValue();

        String problema = validar(nombre, apellido, usuario, clave, rol);
        if (problema != null) {
            Mensajes.advertencia("Datos incorrectos", problema);
            return;
        }

        boolean cambioUsuario = original == null || !original.getNombreUsuario().equalsIgnoreCase(usuario);
        if (cambioUsuario && usuarioDAO.existeUsuario(usuario)) {
            Mensajes.advertencia("Usuario repetido", "El nombre de usuario \"" + usuario + "\" ya está ocupado.");
            return;
        }

        Usuario actual = Sesion.getUsuario();
        boolean soyYo = original != null && actual != null && actual.getIdUsuario() == original.getIdUsuario();
        if (soyYo && !ADMIN.equals(rol)) {
            Mensajes.advertencia("Cambio de rol", "No puedes quitarte el rol de administrador a ti mismo.");
            return;
        }

        boolean dejaDeSerAdmin = original != null && original.esAdministrador() && original.estaActivo() && !ADMIN.equals(rol);
        if (dejaDeSerAdmin && contarAdministradoresActivos() <= 1) {
            Mensajes.advertencia("Cambio de rol", "Debe quedar al menos un administrador activo en el sistema.");
            return;
        }

        Usuario u = new Usuario(0, nombre, apellido, usuario, clave, rol, "Activo");
        boolean listo;
        if (original == null) {
            listo = usuarioDAO.insertar(u);
            if (listo) {
                Mensajes.info("Usuario creado", "Se creó el usuario \"" + usuario + "\" como " + rol + ".");
            }
        } else {
            u.setIdUsuario(original.getIdUsuario());
            u.setEstado(original.getEstado());
            listo = usuarioDAO.actualizar(u);
            if (listo && soyYo) {
                actual.setNombre(nombre);
                actual.setApellido(apellido);
                actual.setNombreUsuario(usuario);
            }
        }

        if (!listo) {
            Mensajes.error("Error", "No se pudo guardar el usuario. Revisa la conexión con la base de datos.");
            return;
        }

        PanelPrincipalController.getInstancia().cerrarSubpanel();
        if (alGuardar != null) {
            alGuardar.accept(usuario);
        }
    }

    @FXML
    void onCancelClick(ActionEvent event) {
        PanelPrincipalController.getInstancia().cerrarSubpanel();
    }

    private int contarAdministradoresActivos() {
        int cuenta = 0;
        for (Usuario u : usuarioDAO.listar()) {
            if (u.esAdministrador() && u.estaActivo()) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private String validar(String nombre, String apellido, String usuario, String clave, String rol) {
        if (nombre.isEmpty() || apellido.isEmpty()) {
            return "Escribe el nombre y el apellido.";
        }
        if (nombre.length() > 100 || apellido.length() > 100) {
            return "El nombre y el apellido pueden tener máximo 100 letras.";
        }
        if (usuario.isEmpty()) {
            return "Escribe el nombre de usuario con el que va a entrar.";
        }
        if (usuario.length() > 50 || !usuario.matches("[A-Za-z0-9._-]+")) {
            return "El usuario solo puede tener letras, números, punto, guion o guion bajo, sin espacios (máximo 50).";
        }
        boolean claveObligatoria = original == null;
        if (claveObligatoria && clave.isEmpty()) {
            return "Escribe una contraseña.";
        }
        if (!clave.isEmpty() && clave.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres.";
        }
        if (rol == null) {
            return "Escoge el rol: Administrador o Empleado.";
        }
        return null;
    }
}
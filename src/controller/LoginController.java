package controller;

import DAO.UsuarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.Sesion;
import model.Usuario;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnIniciarSesion;

    @FXML
    private Label Text_password_ol;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();


    @FXML
    private void initialize() {

        txtUsuario.setOnAction(e -> txtPassword.requestFocus());
        txtPassword.setOnAction(this::onIniciarSesionClick);
        Text_password_ol.setStyle("-fx-cursor: hand;");
    }


    @FXML
    void onIniciarSesionClick(ActionEvent event) {
        String usuarioTexto = txtUsuario.getText().trim();
        String passwordTexto = txtPassword.getText().trim();

        if (usuarioTexto.isEmpty() || passwordTexto.isEmpty()) {
            mostrarAlerta("Campos requeridos", "Por favor ingrese el usuario y la contraseña.", Alert.AlertType.WARNING);
            return;
        }

        Usuario usuario = usuarioDAO.validarLogin(usuarioTexto, passwordTexto);

        if (usuario != null) {

            Sesion.iniciar(usuario);
            abrirPanelPrincipal(event);
        } else {
            mostrarAlerta("Error de Autenticación", "Usuario o contraseña incorrectos.", Alert.AlertType.ERROR);

            txtPassword.clear();
            txtPassword.requestFocus();
        }
    }

    @FXML
    void OnPassword_LO(MouseEvent event) {
        mostrarAlerta("Recuperar contraseña",
                "Pide a la administradora que te asigne una contraseña nueva desde Gestión de usuarios.",
                Alert.AlertType.INFORMATION);
    }

    private void abrirPanelPrincipal(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/vista/PanelPrincipal.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Variedades Lemus - Sistema Principal");
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el panel principal.\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
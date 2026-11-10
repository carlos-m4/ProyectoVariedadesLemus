import DAO.Conexion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/vista/Login.fxml"));

        primaryStage.setTitle("Variedades Lemus - Inicio de Sesión");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(520);
        primaryStage.setMaximized(true);
        primaryStage.show();

        if (!Conexion.probarConexion()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Sin conexión");
            alerta.setHeaderText(null);
            alerta.setContentText("No se pudo conectar con la base de datos.\n"
                    + "Revisa que MySQL esté encendido en XAMPP.");
            alerta.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
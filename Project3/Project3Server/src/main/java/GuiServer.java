import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.HashMap;

public class GuiServer extends Application {

	HashMap<String, Scene> sceneMap;
	Server serverConnection;
	ListView<String> listItems;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems = new ListView<String>();

		serverConnection = new Server(data -> {
			Platform.runLater(() -> {
				listItems.getItems().add(data.toString());
				// Auto scroll to bottom
				listItems.scrollTo(listItems.getItems().size() - 1);
			});
		});

		sceneMap = new HashMap<>();
		sceneMap.put("server", createServerGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(sceneMap.get("server"));
		primaryStage.setTitle("Checkers Server");
		primaryStage.show();
	}

	public Scene createServerGui() {
		Label title = new Label("Checkers Server Log");
		title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: serif;");

		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(15));
		pane.setStyle("-fx-background-color: #1e1e1e;");
		pane.setTop(title);
		pane.setCenter(listItems);

		listItems.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-font-family: monospace;");
		title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

		return new Scene(pane, 500, 400);
	}
}
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.HashMap;

public class GuiServer extends Application {

	// ── Cyberpunk / Retro Arcade palette (mirrors GuiClient) ──
	static final String BG_DARK    = "#0a0a12";
	static final String BG_MID     = "#10101e";
	static final String BG_PANEL   = "#12121f";
	static final String NEON_CYAN  = "#00ffe7";
	static final String NEON_PINK  = "#ff2d78";
	static final String NEON_YELLOW= "#ffe600";
	static final String NEON_PURPLE= "#bf00ff";
	static final String TEXT_BRIGHT= "#e8f0ff";
	static final String TEXT_DIM   = "#6a7a9b";
	static final String FONT_MONO  = "Courier New";

	HashMap<String, Scene> sceneMap;
	Server serverConnection;
	ListView<String> listItems;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems = new ListView<>();

		serverConnection = new Server(data -> {
			Platform.runLater(() -> {
				listItems.getItems().add(data.toString());
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
		primaryStage.setTitle("[ CHECKERS SERVER // LIVE LOG ]");
		primaryStage.show();
	}

	public Scene createServerGui() {

		// ── Top bar ─────────────────────────────────────────────
		Label arcadeTag = new Label("★ ARCADE CHECKERS ★");
		arcadeTag.setStyle(
				"-fx-font-size: 11px; -fx-text-fill: " + NEON_PURPLE + "; "
						+ "-fx-font-family: '" + FONT_MONO + "';"
		);

		Label title = new Label("SERVER LOG");
		title.setStyle(
				"-fx-font-size: 30px; -fx-font-weight: bold; "
						+ "-fx-text-fill: " + NEON_CYAN + "; "
						+ "-fx-font-family: '" + FONT_MONO + "';"
		);

		Label subtitle = new Label("LIVE  //  REAL-TIME EVENT STREAM");
		subtitle.setStyle(
				"-fx-font-size: 11px; -fx-text-fill: " + NEON_PINK + "; "
						+ "-fx-font-family: '" + FONT_MONO + "';"
		);

		// Neon separator line
		Label sep = new Label("══════════════════════════════════════════");
		sep.setStyle(
				"-fx-text-fill: " + NEON_PURPLE + "; "
						+ "-fx-font-family: '" + FONT_MONO + "'; -fx-font-size: 11px;"
		);

		VBox headerBox = new VBox(4, arcadeTag, title, subtitle, sep);
		headerBox.setAlignment(Pos.CENTER_LEFT);
		headerBox.setPadding(new Insets(18, 16, 10, 16));
		headerBox.setStyle("-fx-background-color: " + BG_PANEL + ";");

		// ── Log list ─────────────────────────────────────────────
		listItems.setStyle(
				"-fx-background-color: " + BG_MID + "; "
						+ "-fx-control-inner-background: " + BG_MID + "; "
						+ "-fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 1px;"
		);

		listItems.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("-fx-background-color: " + BG_MID + ";");
				} else {
					// Color-code log lines by content keywords
					String color;
					if (item.toLowerCase().contains("error") || item.toLowerCase().contains("invalid")) {
						color = NEON_PINK;
					} else if (item.toLowerCase().contains("game") || item.toLowerCase().contains("start")) {
						color = NEON_CYAN;
					} else if (item.toLowerCase().contains("win") || item.toLowerCase().contains("over")) {
						color = NEON_YELLOW;
					} else {
						color = TEXT_BRIGHT;
					}
					setText("▶  " + item);
					setStyle(
							"-fx-text-fill: " + color + "; "
									+ "-fx-font-size: 13px; "
									+ "-fx-font-family: '" + FONT_MONO + "'; "
									+ "-fx-background-color: " + BG_MID + "; "
									+ "-fx-padding: 5px 8px;"
					);
				}
			}
		});

		// ── Status bar ───────────────────────────────────────────
		Label statusBar = new Label("  SERVER RUNNING  //  AWAITING CONNECTIONS");
		statusBar.setStyle(
				"-fx-background-color: " + BG_PANEL + "; "
						+ "-fx-text-fill: " + NEON_YELLOW + "; "
						+ "-fx-font-size: 12px; -fx-font-weight: bold; "
						+ "-fx-font-family: '" + FONT_MONO + "'; "
						+ "-fx-padding: 7px 14px; "
						+ "-fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 1px 0 0 0;"
		);

		BorderPane pane = new BorderPane();
		pane.setTop(headerBox);
		pane.setCenter(listItems);
		pane.setBottom(statusBar);
		pane.setPadding(new Insets(0));
		pane.setStyle("-fx-background-color: " + BG_DARK + ";");

		return new Scene(pane, 560, 440);
	}
}
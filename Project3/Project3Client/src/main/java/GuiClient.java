import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.HashMap;
import java.util.List;

public class GuiClient extends Application {

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	Stage primaryStage;

	String username = null;
	String redPlayer = null;
	String blackPlayer = null;
	int[][] board = null;
	int currentTurn = -1;
	int myColor = -1;

	// Selected piece
	int selectedRow = -1;
	int selectedCol = -1;
	boolean pieceSelected = false;
	boolean multiJumpActive = false;

	Canvas boardCanvas;
	Label statusLabel;
	Label turnLabel;

	ListView<String> chatList;
	TextField chatInput;
	Button chatSendBtn;
	Button toggleChat;
	VBox chatPanel;

	// Lobby
	ListView<String> lobbyPlayerList;
	Label lobbyStatus;

	static final int TILE = 75;
	static final int BOARD_SIZE = 8 * TILE;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		this.primaryStage = stage;

		clientConnection = new Client(data -> {
			Platform.runLater(() -> handleIncoming((Message) data));
		});
		clientConnection.start();

		sceneMap = new HashMap<>();
		sceneMap.put("username", createUsernameScene());
		sceneMap.put("lobby", createLobbyScene());
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(sceneMap.get("username"));
		primaryStage.setTitle("Checkers");
		primaryStage.show();
	}

	// ─────────────────────────────────────────────
	// ─────────────────────────────────────────────
	//  USERNAME SCENE (inline)
	// ─────────────────────────────────────────────

	Label usernameErrLabel = new Label("");

	private Scene createUsernameScene() {
		Label title = new Label("\u265f CHECKERS");
		title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: serif;");

		Label subtitle = new Label("Online Multiplayer");
		subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaaaaa;");

		TextField nameField = new TextField();
		nameField.setPromptText("Enter your username...");
		nameField.setMaxWidth(260);
		nameField.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: white; -fx-prompt-text-fill: gray; -fx-font-size: 14px; -fx-padding: 10px;");

		Button okBtn = new Button("Enter Lobby  \u2192");
		okBtn.setMaxWidth(260);
		okBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px;");

		usernameErrLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

		okBtn.setOnAction(e -> {
			String name = nameField.getText().trim();
			if (name.isEmpty()) { usernameErrLabel.setText("Username cannot be empty."); return; }
			okBtn.setDisable(true);
			usernameErrLabel.setText("");
			clientConnection.send(Message.setUsername(name));
		});
		nameField.setOnAction(e -> okBtn.fire());

		VBox box = new VBox(16, title, subtitle, nameField, okBtn, usernameErrLabel);
		box.setPadding(new Insets(80, 60, 40, 60));
		box.setStyle("-fx-background-color: #1e1e1e;");
		box.setAlignment(Pos.CENTER);

		return new Scene(box, 420, 520);
	}


	// ─────────────────────────────────────────────
	//  LOBBY SCENE
	// ─────────────────────────────────────────────

	private Scene createLobbyScene() {
		Label title = new Label("♟ CHECKERS LOBBY");
		title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: serif;");

		Label subtitle = new Label("Challenge a player to start a game");
		subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaaaaa;");

		VBox titleBox = new VBox(6, title, subtitle);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(30, 0, 20, 0));

		Label playersLabel = new Label("🟢  Players Online");
		playersLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #cccccc;");

		lobbyPlayerList = new ListView<>();
		lobbyPlayerList.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b;");
		lobbyPlayerList.setPrefHeight(220);

		lobbyPlayerList.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("-fx-background-color: #2b2b2b;");
				} else {
					setText("  ♟  " + item);
					if (isSelected()) {
						setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-background-color: #e74c3c; -fx-padding: 8px; -fx-font-weight: bold;");
					} else {
						setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-background-color: #2b2b2b; -fx-padding: 8px;");
					}
				}
			}

			@Override
			public void updateSelected(boolean selected) {
				super.updateSelected(selected);
				updateItem(getItem(), isEmpty());
			}
		});

		Button challengeBtn = new Button("⚔  Challenge Selected Player");
		challengeBtn.setMaxWidth(Double.MAX_VALUE);
		challengeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px;");
		challengeBtn.setOnAction(e -> {
			String selected = lobbyPlayerList.getSelectionModel().getSelectedItem();
			if (selected == null) { lobbyStatus.setText("Select a player first!"); return; }
			clientConnection.send(Message.challengeSend(username, selected));
			lobbyStatus.setText("Challenge sent to " + selected + "...");
		});

		lobbyStatus = new Label("");
		lobbyStatus.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 13px;");
		lobbyStatus.setWrapText(true);

		VBox centerBox = new VBox(12, playersLabel, lobbyPlayerList, challengeBtn, lobbyStatus);
		centerBox.setPadding(new Insets(10, 40, 20, 40));

		VBox root = new VBox(titleBox, centerBox);
		root.setStyle("-fx-background-color: #1e1e1e;");
		VBox.setVgrow(centerBox, Priority.ALWAYS);

		return new Scene(root, 420, 520);
	}

	// ─────────────────────────────────────────────
	//  INCOMING MESSAGE HANDLER
	// ─────────────────────────────────────────────

	private void handleIncoming(Message msg) {
		switch (msg.getType()) {

			case username_accepted: {
				username = msg.getContent();
				Object ud = primaryStage.getUserData();
				if (ud instanceof Stage) ((Stage) ud).close();
				primaryStage.setTitle("Checkers — " + username);
				primaryStage.setScene(sceneMap.get("lobby"));
				primaryStage.setWidth(420);
				primaryStage.setHeight(520);
				lobbyStatus.setText("Welcome, " + username + "! Waiting for players...");
				break;
			}

			case username_taken: {
				usernameErrLabel.setText(msg.getContent());
				primaryStage.setScene(sceneMap.get("username"));
				break;
			}

			case lobby_update: {
				List<String> players = msg.getPlayerList();
				players.removeIf(p -> p.equals(username));
				lobbyPlayerList.getItems().setAll(players);
				lobbyStatus.setText(players.isEmpty() ? "No other players online yet." : players.size() + " player(s) available.");
				break;
			}

			case challenge_receive: {
				String challenger = msg.getSenderUsername();
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Challenge Received!");
				alert.setHeaderText("⚔ " + challenger + " wants to play!");
				alert.setContentText("Do you accept the challenge?");
				ButtonType accept = new ButtonType("Accept ✅");
				ButtonType decline = new ButtonType("Decline ❌");
				alert.getButtonTypes().setAll(accept, decline);
				alert.showAndWait().ifPresent(result -> {
					if (result == accept) {
						clientConnection.send(Message.challengeAccept(username, challenger));
					} else {
						clientConnection.send(Message.challengeDecline(username, challenger));
					}
				});
				break;
			}


			case waiting_for_opponent: {
				lobbyStatus.setText("Waiting for another player to join...");
				break;
			}

			case game_start: {
				redPlayer = msg.getRedPlayer();
				blackPlayer = msg.getBlackPlayer();
				board = msg.getBoard();
				currentTurn = CheckersConstants.RED;
				myColor = username.equals(redPlayer) ? CheckersConstants.RED : CheckersConstants.BLACK;
				statusLabel.setText("Game started! You are " + (myColor == CheckersConstants.RED ? "🔴 RED" : "⚫ BLACK"));
				multiJumpActive = false;
				selectedRow = -1;
				selectedCol = -1;
				pieceSelected = false;
				updateTurnLabel();
				drawBoard();
				chatSendBtn.setDisable(false);
				chatList.getItems().clear();
				primaryStage.setScene(sceneMap.get("client"));
				primaryStage.setWidth(BOARD_SIZE + 40);
				primaryStage.setHeight(BOARD_SIZE + 80);
				break;
			}

			case game_state: {
				board = msg.getBoard();
				currentTurn = msg.getCurrentTurn();
				int mjRow = msg.getMultiJumpRow();
				int mjCol = msg.getMultiJumpCol();

				if (currentTurn == myColor && mjRow != -1) {
					selectedRow = mjRow;
					selectedCol = mjCol;
					pieceSelected = true;
					multiJumpActive = true;
					statusLabel.setText("Double jump! Keep jumping with the highlighted piece.");
				} else {
					selectedRow = -1;
					selectedCol = -1;
					pieceSelected = false;
					multiJumpActive = false;
				}
				updateTurnLabel();
				drawBoard();
				break;
			}

			case invalid_move: {
				statusLabel.setText("Invalid move: " + msg.getContent());
				selectedRow = -1;
				selectedCol = -1;
				pieceSelected = false;
				drawBoard();
				break;
			}

			case game_over: {
				board = null;
				drawBoard();
				statusLabel.setText("Game Over! " + msg.getContent());
				turnLabel.setText("");

				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Game Over");
				alert.setHeaderText("🏆 " + msg.getContent());
				alert.setContentText("Would you like a rematch?");
				ButtonType rematch = new ButtonType("Rematch 🔄");
				ButtonType backToLobby = new ButtonType("Back to Lobby");
				alert.getButtonTypes().setAll(rematch, backToLobby);
				alert.showAndWait().ifPresent(result -> {
					if (result == rematch) {
						clientConnection.send(Message.rematchRequest(username));
						statusLabel.setText("Rematch requested... waiting for opponent.");
					} else {
						clientConnection.send(Message.rematchDecline(username));
						returnToLobby();
					}
				});
				break;
			}

			case rematch_offer: {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Rematch?");
				alert.setHeaderText("🔄 " + msg.getContent());
				alert.setContentText("Do you want to play again?");
				ButtonType yes = new ButtonType("Yes! 🔄");
				ButtonType no = new ButtonType("No thanks");
				alert.getButtonTypes().setAll(yes, no);
				alert.showAndWait().ifPresent(result -> {
					if (result == yes) {
						clientConnection.send(Message.rematchAccept(username));
					} else {
						clientConnection.send(Message.rematchDecline(username));
						returnToLobby();
					}
				});
				break;
			}

			case challenge_declined: {
				// Also used when opponent declines rematch
				if (primaryStage.getScene() == sceneMap.get("client")) {
					statusLabel.setText(msg.getContent());
					returnToLobby();
				} else {
					lobbyStatus.setText(msg.getContent());
				}
				break;
			}

			case chat: {
				chatList.getItems().add(msg.getSenderUsername() + ": " + msg.getContent());
				chatList.scrollTo(chatList.getItems().size() - 1);
				if (!chatPanel.isVisible()) {
					String current = toggleChat.getText();
					int unread = 1;
					if (current.contains("(")) {
						try { unread = Integer.parseInt(current.replaceAll(".*\\((\\d+)\\).*", "$1")) + 1; }
						catch (NumberFormatException ignored) {}
					}
					toggleChat.setText("💬 Chat ▶ (" + unread + ")");
					toggleChat.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px;");
				}
				break;
			}

			default:
				if (statusLabel != null) statusLabel.setText("Unknown message: " + msg);
		}
	}

	private void returnToLobby() {
		board = null;
		myColor = -1;
		currentTurn = -1;
		redPlayer = null;
		blackPlayer = null;
		pieceSelected = false;
		multiJumpActive = false;
		selectedRow = -1;
		selectedCol = -1;
		drawBoard();
		primaryStage.setScene(sceneMap.get("lobby"));
		primaryStage.setWidth(420);
		primaryStage.setHeight(520);
	}

	// ─────────────────────────────────────────────
	//  BOARD CLICK HANDLER (unchanged from your version)
	// ─────────────────────────────────────────────

	private void handleBoardClick(double x, double y) {
		if (board == null) return;
		if (myColor != currentTurn) {
			statusLabel.setText("It's not your turn!");
			return;
		}

		int dispCol = (int) (x / TILE);
		int dispRow = (int) (y / TILE);
		if (dispRow < 0 || dispRow > 7 || dispCol < 0 || dispCol > 7) return;

		int row = toActualRow(dispRow);
		int col = toActualCol(dispCol);
		int piece = board[row][col];

		if (!pieceSelected) {
			if (piece == myColor || piece == getKingColor(myColor)) {
				selectedRow = row;
				selectedCol = col;
				pieceSelected = true;
				statusLabel.setText("Selected piece at (" + row + ", " + col + "). Click destination.");
				drawBoard();
			} else {
				statusLabel.setText("Select one of your pieces first.");
			}
		} else {
			if (row == selectedRow && col == selectedCol) {
				if (multiJumpActive) {
					statusLabel.setText("You must continue jumping with this piece!");
					return;
				}
				pieceSelected = false;
				selectedRow = -1;
				selectedCol = -1;
				drawBoard();
				return;
			}
			clientConnection.send(Message.makeMove(username, selectedRow, selectedCol, row, col));
			if (!multiJumpActive) {
				pieceSelected = false;
				selectedRow = -1;
				selectedCol = -1;
			}
		}
	}

	private int getKingColor(int color) {
		if (color == CheckersConstants.RED) return CheckersConstants.RED_KING;
		if (color == CheckersConstants.BLACK) return CheckersConstants.BLACK_KING;
		return CheckersConstants.EMPTY;
	}

	// ─────────────────────────────────────────────
	//  DRAW BOARD (unchanged from your version)
	// ─────────────────────────────────────────────

	private void drawBoard() {
		GraphicsContext gc = boardCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, BOARD_SIZE, BOARD_SIZE);

		if (board == null) {
			gc.setFill(Color.GRAY);
			gc.fillRect(0, 0, BOARD_SIZE, BOARD_SIZE);
			gc.setFill(Color.WHITE);
			gc.setFont(Font.font("serif", FontWeight.BOLD, 24));
			gc.fillText("Waiting for game...", 130, BOARD_SIZE / 2.0);
			return;
		}

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int dispRow = toDisplayRow(row);
				int dispCol = toDisplayCol(col);
				boolean isDark = (dispRow + dispCol) % 2 == 1;
				gc.setFill(isDark ? Color.SADDLEBROWN : Color.WHEAT);
				gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);

				int piece = board[row][col];
				if (piece != CheckersConstants.EMPTY) {
					drawPiece(gc, dispRow, dispCol, piece);
				}

				// Draw highlight on top of piece so it's visible
				if (pieceSelected && row == selectedRow && col == selectedCol) {
					gc.setFill(Color.color(1, 1, 0, 0.45));
					gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);
				}
			}
		}
	}

	private void drawPiece(GraphicsContext gc, int row, int col, int piece) {
		double x = col * TILE + TILE * 0.1;
		double y = row * TILE + TILE * 0.1;
		double size = TILE * 0.8;

		gc.setFill(Color.color(0, 0, 0, 0.3));
		gc.fillOval(x + 3, y + 3, size, size);

		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setFill(Color.CRIMSON);
		} else {
			gc.setFill(Color.color(0.1, 0.1, 0.1));
		}
		gc.fillOval(x, y, size, size);

		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setStroke(Color.LIGHTCORAL);
		} else {
			gc.setStroke(Color.DIMGRAY);
		}
		gc.setLineWidth(2);
		gc.strokeOval(x + 4, y + 4, size - 8, size - 8);

		if (piece == CheckersConstants.RED_KING || piece == CheckersConstants.BLACK_KING) {
			gc.setFill(Color.GOLD);
			gc.setFont(Font.font("serif", FontWeight.BOLD, 20));
			gc.fillText("♛", col * TILE + TILE * 0.3, row * TILE + TILE * 0.65);
		}
	}

	private void updateTurnLabel() {
		if (currentTurn == myColor) {
			turnLabel.setText("YOUR TURN");
			turnLabel.setStyle("-fx-text-fill: green; -fx-font-size: 16px; -fx-font-weight: bold;");
		} else {
			String otherName = (myColor == CheckersConstants.RED) ? blackPlayer : redPlayer;
			turnLabel.setText(otherName + "'s turn");
			turnLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 16px;");
		}
	}

	private int toDisplayRow(int actualRow) { return myColor == CheckersConstants.BLACK ? 7 - actualRow : actualRow; }
	private int toDisplayCol(int actualCol) { return myColor == CheckersConstants.BLACK ? 7 - actualCol : actualCol; }
	private int toActualRow(int displayRow) { return myColor == CheckersConstants.BLACK ? 7 - displayRow : displayRow; }
	private int toActualCol(int displayCol) { return myColor == CheckersConstants.BLACK ? 7 - displayCol : displayCol; }

	// ─────────────────────────────────────────────
	//  GAME GUI (unchanged from your version)
	// ─────────────────────────────────────────────

	public Scene createClientGui() {
		boardCanvas = new Canvas(BOARD_SIZE, BOARD_SIZE);
		boardCanvas.setOnMouseClicked(e -> handleBoardClick(e.getX(), e.getY()));
		drawBoard();

		statusLabel = new Label("Connecting to server...");
		statusLabel.setFont(Font.font("serif", 14));
		statusLabel.setWrapText(true);

		turnLabel = new Label("");
		turnLabel.setFont(Font.font("serif", FontWeight.BOLD, 16));

		VBox bottomBar = new VBox(6, turnLabel, statusLabel);
		bottomBar.setAlignment(Pos.CENTER);
		bottomBar.setPadding(new Insets(10));

		Label chatTitle = new Label("Chat");
		chatTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

		chatList = new ListView<>();
		chatList.setPrefWidth(220);
		chatList.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-text-fill: white;");
		VBox.setVgrow(chatList, Priority.ALWAYS);

		chatInput = new TextField();
		chatInput.setPromptText("Type a message...");
		chatInput.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: white; -fx-prompt-text-fill: gray;");

		chatSendBtn = new Button("Send");
		chatSendBtn.setMaxWidth(Double.MAX_VALUE);
		chatSendBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
		chatSendBtn.setDisable(true);

		Runnable sendChat = () -> {
			String text = chatInput.getText().trim();
			if (text.isEmpty() || username == null) return;
			chatList.getItems().add("You: " + text);
			chatList.scrollTo(chatList.getItems().size() - 1);
			clientConnection.send(Message.chat(username, text));
			chatInput.clear();
		};

		chatSendBtn.setOnAction(e -> sendChat.run());
		chatInput.setOnAction(e -> sendChat.run());

		HBox chatInputRow = new HBox(6, chatInput, chatSendBtn);
		HBox.setHgrow(chatInput, Priority.ALWAYS);
		chatInputRow.setPadding(new Insets(4, 0, 0, 0));

		chatPanel = new VBox(8, chatTitle, chatList, chatInputRow);
		chatPanel.setVisible(false);
		chatPanel.setManaged(false);
		chatPanel.setPadding(new Insets(10));
		chatPanel.setStyle("-fx-background-color: #1e1e1e;");
		chatPanel.setPrefWidth(240);

		BorderPane root = new BorderPane();
		root.setCenter(boardCanvas);
		root.setBottom(bottomBar);

		toggleChat = new Button("💬 Chat ▶");
		toggleChat.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 11px;");
		toggleChat.setOnAction(e -> {
			boolean visible = chatPanel.isVisible();
			chatPanel.setVisible(!visible);
			chatPanel.setManaged(!visible);
			if (!visible) {
				toggleChat.setText("💬 Chat ◀");
				toggleChat.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 11px;");
			} else {
				toggleChat.setText("💬 Chat ▶");
				toggleChat.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 11px;");
			}
			Stage stage = (Stage) root.getScene().getWindow();
			stage.setWidth(visible ? BOARD_SIZE + 40 : BOARD_SIZE + 240);
		});

		HBox topBar = new HBox(toggleChat);
		topBar.setAlignment(Pos.CENTER_RIGHT);
		topBar.setPadding(new Insets(4, 8, 0, 0));
		topBar.setStyle("-fx-background-color: #2b2b2b;");

		root.setTop(topBar);
		root.setRight(chatPanel);
		root.setStyle("-fx-background-color: #2b2b2b;");

		return new Scene(root, BOARD_SIZE + 40, BOARD_SIZE + 80);
	}
}
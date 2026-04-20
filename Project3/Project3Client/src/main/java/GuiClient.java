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

	static final String BG_DARK      = "#0a0a0f";
	static final String BG_MID       = "#0f0f1a";
	static final String BG_PANEL     = "#12121f";
	static final String NEON_PINK    = "#ff2d78";
	static final String NEON_CYAN    = "#00f5ff";
	static final String NEON_PURPLE  = "#bf00ff";
	static final String NEON_YELLOW  = "#ffe600";
	static final String NEON_GREEN   = "#00ff9f";
	static final String TEXT_DIM     = "#7a7a9a";
	static final String TEXT_BRIGHT  = "#e0e0ff";

	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	Stage primaryStage;

	String username = null;
	String redPlayer = null;
	String blackPlayer = null;
	int[][] board = null;
	int currentTurn = -1;
	int myColor = -1;

	int selectedRow = -1;
	int selectedCol = -1;
	boolean pieceSelected = false;
	boolean multiJumpActive = false;
	boolean rematchRequested = false;

	Label usernameErrLabel = new Label("");
	Button usernameBtn = null; // field so username_taken can re-enable it

	Canvas boardCanvas;
	Label statusLabel;
	Label turnLabel;

	ListView<String> chatList;
	TextField chatInput;
	Button chatSendBtn;
	Button toggleChat;
	VBox chatPanel;

	ListView<String> lobbyPlayerList;
	Label lobbyStatus;

	static final int TILE = 75;
	static final int BOARD_SIZE = 8 * TILE;

	SoundManager sound = new SoundManager();
	Button lobbyMuteBtn;
	Button gameMuteBtn;

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
		primaryStage.setTitle("CHECKERS // ARCADE");
		primaryStage.show();
	}

	private String neonBtn(String bg) {
		return "-fx-background-color: " + bg + "; " +
				"-fx-text-fill: " + BG_DARK + "; " +
				"-fx-font-family: monospace; " +
				"-fx-font-weight: bold; " +
				"-fx-font-size: 13px; " +
				"-fx-padding: 10px 16px; " +
				"-fx-cursor: hand;";
	}

	private String glowLabel(String color) {
		return "-fx-text-fill: " + color + "; " +
				"-fx-font-family: monospace; " +
				"-fx-font-weight: bold; ";
	}

	// ─────────────────────────────────────────────
	//  USERNAME SCENE
	// ─────────────────────────────────────────────

	private Scene createUsernameScene() {
		Label arcade = new Label("[ CHECKERS ARCADE ]");
		arcade.setStyle("-fx-text-fill: " + NEON_CYAN + "; -fx-font-family: monospace; -fx-font-size: 11px; -fx-font-weight: bold;");

		Label title = new Label("INSERT COIN");
		title.setStyle("-fx-text-fill: " + NEON_PINK + "; -fx-font-family: monospace; -fx-font-size: 36px; -fx-font-weight: bold;");

		Label subtitle = new Label("// ONLINE MULTIPLAYER //");
		subtitle.setStyle("-fx-text-fill: " + NEON_PURPLE + "; -fx-font-family: monospace; -fx-font-size: 12px;");

		Label prompt = new Label("> ENTER PLAYER TAG:");
		prompt.setStyle(glowLabel(NEON_CYAN) + "-fx-font-size: 12px;");

		TextField nameField = new TextField();
		nameField.setPromptText("player_one");
		nameField.setMaxWidth(280);
		nameField.setStyle(
				"-fx-background-color: #1a1a2e; " +
						"-fx-text-fill: " + NEON_GREEN + "; " +
						"-fx-prompt-text-fill: #444466; " +
						"-fx-font-family: monospace; " +
						"-fx-font-size: 15px; " +
						"-fx-padding: 10px; " +
						"-fx-border-color: " + NEON_CYAN + "; " +
						"-fx-border-width: 1px;"
		);

		// Store as field so username_taken can re-enable it
		usernameBtn = new Button(">> START GAME <<");
		usernameBtn.setMaxWidth(280);
		usernameBtn.setStyle(neonBtn(NEON_PINK));

		usernameErrLabel.setStyle("-fx-text-fill: " + NEON_PINK + "; -fx-font-family: monospace; -fx-font-size: 11px;");
		usernameErrLabel.setText("");

		usernameBtn.setOnAction(e -> {
			String name = nameField.getText().trim();
			if (name.isEmpty()) { usernameErrLabel.setText("! USERNAME CANNOT BE EMPTY"); return; }
			usernameBtn.setDisable(true);
			usernameErrLabel.setText("");
			clientConnection.send(Message.setUsername(name));
		});
		nameField.setOnAction(e -> usernameBtn.fire());

		Label scanline = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		scanline.setStyle("-fx-text-fill: #222244; -fx-font-family: monospace; -fx-font-size: 10px;");

		VBox box = new VBox(14, arcade, title, subtitle, scanline, prompt, nameField, usernameBtn, usernameErrLabel);
		box.setPadding(new Insets(70, 60, 40, 60));
		box.setStyle("-fx-background-color: " + BG_DARK + ";");
		box.setAlignment(Pos.CENTER);

		return new Scene(box, 420, 520);
	}

	// ─────────────────────────────────────────────
	//  LOBBY SCENE
	// ─────────────────────────────────────────────

	private Scene createLobbyScene() {
		Label arcade = new Label("[ CHECKERS ARCADE ]");
		arcade.setStyle("-fx-text-fill: " + NEON_CYAN + "; -fx-font-family: monospace; -fx-font-size: 11px;");

		Label title = new Label("★ PLAYER SELECT ★");
		title.setStyle("-fx-text-fill: " + NEON_PINK + "; -fx-font-family: monospace; -fx-font-size: 26px; -fx-font-weight: bold;");

		Label subtitle = new Label("// CHOOSE YOUR OPPONENT //");
		subtitle.setStyle("-fx-text-fill: " + NEON_PURPLE + "; -fx-font-family: monospace; -fx-font-size: 11px;");

		VBox titleBox = new VBox(6, arcade, title, subtitle);
		titleBox.setAlignment(Pos.CENTER);
		titleBox.setPadding(new Insets(24, 0, 16, 0));

		Label playersLabel = new Label("> PLAYERS ONLINE:");
		playersLabel.setStyle(glowLabel(NEON_CYAN) + "-fx-font-size: 12px;");

		lobbyPlayerList = new ListView<>();
		lobbyPlayerList.setStyle(
				"-fx-background-color: #0d0d1a; " +
						"-fx-control-inner-background: #0d0d1a; " +
						"-fx-border-color: " + NEON_PURPLE + "; " +
						"-fx-border-width: 1px;"
		);
		lobbyPlayerList.setPrefHeight(200);

		lobbyPlayerList.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setStyle("-fx-background-color: #0d0d1a;");
				} else {
					setText("  ▶  " + item);
					if (isSelected()) {
						setStyle("-fx-text-fill: " + BG_DARK + "; -fx-font-family: monospace; -fx-font-size: 14px; -fx-background-color: " + NEON_CYAN + "; -fx-padding: 8px; -fx-font-weight: bold;");
					} else {
						setStyle("-fx-text-fill: " + NEON_GREEN + "; -fx-font-family: monospace; -fx-font-size: 14px; -fx-background-color: #0d0d1a; -fx-padding: 8px;");
					}
				}
			}

			@Override
			public void updateSelected(boolean selected) {
				super.updateSelected(selected);
				updateItem(getItem(), isEmpty());
			}
		});

		Button challengeBtn = new Button("⚔  CHALLENGE PLAYER  ⚔");
		challengeBtn.setMaxWidth(Double.MAX_VALUE);
		challengeBtn.setStyle(neonBtn(NEON_PINK));
		challengeBtn.setOnAction(e -> {
			String selected = lobbyPlayerList.getSelectionModel().getSelectedItem();
			if (selected == null) { lobbyStatus.setText("! SELECT A PLAYER FIRST"); return; }
			clientConnection.send(Message.challengeSend(username, selected));
			lobbyStatus.setText("> CHALLENGE SENT TO " + selected.toUpperCase() + "...");
		});

		lobbyStatus = new Label("");
		lobbyStatus.setStyle("-fx-text-fill: " + NEON_YELLOW + "; -fx-font-family: monospace; -fx-font-size: 11px;");
		lobbyStatus.setWrapText(true);

		Label strip = new Label("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
		strip.setStyle("-fx-text-fill: #1a1a3a; -fx-font-family: monospace; -fx-font-size: 9px;");

		Button lobbyMuteBtn = createMuteBtn(true);
		lobbyMuteBtn.setMaxWidth(Double.MAX_VALUE);

		VBox centerBox = new VBox(10, playersLabel, lobbyPlayerList, challengeBtn, lobbyStatus, lobbyMuteBtn, strip);
		centerBox.setPadding(new Insets(10, 30, 20, 30));

		VBox root = new VBox(titleBox, centerBox);
		root.setStyle("-fx-background-color: " + BG_DARK + ";");
		VBox.setVgrow(centerBox, Priority.ALWAYS);

		return new Scene(root, 420, 520);
	}

	// ─────────────────────────────────────────────
	//  INCOMING MESSAGE HANDLER
	// ─────────────────────────────────────────────

	private void handleIncoming(Message msg) {
		switch (msg.getType()) {

			case username_accepted: {
				sound.playSFX(SoundManager.SFX.USERNAME_ACCEPTED);
				username = msg.getContent();
				primaryStage.setTitle("CHECKERS // " + username.toUpperCase());
				primaryStage.setScene(sceneMap.get("lobby"));
				primaryStage.setWidth(420);
				primaryStage.setHeight(520);
				lobbyStatus.setText("> WELCOME, " + username.toUpperCase() + "!");
				sound.playMusic("music_lobby.mp3");
				break;
			}

			case username_taken: {
				// Show error and re-enable button so they can try again
				usernameErrLabel.setText("! " + msg.getContent().toUpperCase());
				if (usernameBtn != null) usernameBtn.setDisable(false);
				primaryStage.setScene(sceneMap.get("username"));
				break;
			}

			case lobby_update: {
				List<String> players = msg.getPlayerList();
				players.removeIf(p -> p.equals(username));
				lobbyPlayerList.getItems().setAll(players);
				lobbyStatus.setText(players.isEmpty()
						? "> NO PLAYERS ONLINE YET..."
						: "> " + players.size() + " PLAYER(S) AVAILABLE");
				break;
			}

			case challenge_receive: {
				sound.playSFX(SoundManager.SFX.CHALLENGE);
				String challenger = msg.getSenderUsername();
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("INCOMING CHALLENGE");
				alert.setHeaderText("⚔ " + challenger.toUpperCase() + " WANTS TO PLAY!");
				alert.setContentText("Do you accept the challenge?");
				ButtonType accept = new ButtonType("ACCEPT ✅");
				ButtonType decline = new ButtonType("DECLINE ❌");
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

			case challenge_declined: {
				if (primaryStage.getScene() == sceneMap.get("client")) {
					statusLabel.setText(msg.getContent());
					returnToLobby();
				} else {
					lobbyStatus.setText("> " + msg.getContent().toUpperCase());
				}
				break;
			}

			case waiting_for_opponent: {
				lobbyStatus.setText("> WAITING FOR OPPONENT...");
				break;
			}

			case game_start: {
				redPlayer = msg.getRedPlayer();
				blackPlayer = msg.getBlackPlayer();
				board = msg.getBoard();
				currentTurn = CheckersConstants.RED;
				myColor = username.equals(redPlayer) ? CheckersConstants.RED : CheckersConstants.BLACK;
				statusLabel.setText("PLAYER: " + (myColor == CheckersConstants.RED ? "🔴 RED" : "⚫ BLACK"));
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
				sound.playMusic("music_game.mp3");
				sound.playSFX(SoundManager.SFX.GAME_START);
				break;
			}

			case game_state: {
				board = msg.getBoard();
				currentTurn = msg.getCurrentTurn();
				int mjRow = msg.getMultiJumpRow();
				int mjCol = msg.getMultiJumpCol();
				if (currentTurn == myColor) {
					sound.playSFX(SoundManager.SFX.YOUR_TURN);
				}
				if (currentTurn == myColor && mjRow != -1) {
					selectedRow = mjRow;
					selectedCol = mjCol;
					pieceSelected = true;
					multiJumpActive = true;
					statusLabel.setText(">> DOUBLE JUMP! KEEP JUMPING!");
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
				sound.playSFX(SoundManager.SFX.INVALID);
				statusLabel.setText("! INVALID: " + msg.getContent().toUpperCase());
				selectedRow = -1;
				selectedCol = -1;
				pieceSelected = false;
				drawBoard();
				break;
			}

			case game_over: {
				sound.playSFX(SoundManager.SFX.GAME_OVER);
				sound.stopMusic();
				board = null;
				rematchRequested = false;
				drawBoard();
				statusLabel.setText("GAME OVER // " + msg.getContent().toUpperCase());
				turnLabel.setText("");

				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("GAME OVER");
				alert.setHeaderText("🏆 " + msg.getContent().toUpperCase());
				alert.setContentText("Continue playing?");
				ButtonType rematch = new ButtonType("REMATCH 🔄");
				ButtonType backToLobby = new ButtonType("LOBBY");
				alert.getButtonTypes().setAll(rematch, backToLobby);
				alert.showAndWait().ifPresent(result -> {
					if (result == rematch) {
						rematchRequested = true;
						clientConnection.send(Message.rematchRequest(username));
						statusLabel.setText("> REMATCH REQUESTED... WAITING FOR OPPONENT.");
					} else {
						clientConnection.send(Message.rematchDecline(username));
						returnToLobby();
					}
				});
				break;
			}

			case rematch_offer: {
				if (rematchRequested) {
					clientConnection.send(Message.rematchAccept(username));
					break;
				}
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("REMATCH?");
				alert.setHeaderText("🔄 " + msg.getContent().toUpperCase());
				alert.setContentText("Play again?");
				ButtonType yes = new ButtonType("YES 🔄");
				ButtonType no = new ButtonType("NO THANKS");
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

			case chat: {
				sound.playSFX(SoundManager.SFX.CHAT);
				chatList.getItems().add(msg.getSenderUsername() + ": " + msg.getContent());
				chatList.scrollTo(chatList.getItems().size() - 1);
				if (!chatPanel.isVisible()) {
					String current = toggleChat.getText();
					int unread = 1;
					if (current.contains("(")) {
						try { unread = Integer.parseInt(current.replaceAll(".*\\((\\d+)\\).*", "$1")) + 1; }
						catch (NumberFormatException ignored) {}
					}
					toggleChat.setText("💬 CHAT ▶ (" + unread + ")");
					toggleChat.setStyle("-fx-background-color: " + NEON_PINK + "; -fx-text-fill: " + BG_DARK + "; -fx-font-family: monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
				}
				break;
			}

			default:
				if (statusLabel != null) statusLabel.setText("?? " + msg);
		}
	}

	private void returnToLobby() {
		board = null;
		rematchRequested = false;
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
	//  BOARD CLICK HANDLER
	// ─────────────────────────────────────────────

	private void handleBoardClick(double x, double y) {
		if (board == null) return;
		if (myColor != currentTurn) {
			statusLabel.setText("! NOT YOUR TURN");
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
				statusLabel.setText("> PIECE SELECTED. CHOOSE DESTINATION.");
				drawBoard();
			} else {
				statusLabel.setText("! SELECT YOUR OWN PIECE FIRST.");
			}
		} else {
			if (row == selectedRow && col == selectedCol) {
				if (multiJumpActive) {
					statusLabel.setText("! MUST CONTINUE JUMPING!");
					return;
				}
				pieceSelected = false;
				selectedRow = -1;
				selectedCol = -1;
				drawBoard();
				return;
			}
			clientConnection.send(Message.makeMove(username, selectedRow, selectedCol, row, col));
			if (Math.abs(row - selectedRow) == 2) {
				sound.playSFX(SoundManager.SFX.CAPTURE);
			} else {
				sound.playSFX(SoundManager.SFX.MOVE);
			}
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
	//  DRAW BOARD
	// ─────────────────────────────────────────────

	private void drawBoard() {
		GraphicsContext gc = boardCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, BOARD_SIZE, BOARD_SIZE);

		if (board == null) {
			gc.setFill(Color.web(BG_DARK));
			gc.fillRect(0, 0, BOARD_SIZE, BOARD_SIZE);
			gc.setStroke(Color.web(NEON_PURPLE, 0.3));
			gc.setLineWidth(1);
			for (int i = 0; i <= 8; i++) {
				gc.strokeLine(i * TILE, 0, i * TILE, BOARD_SIZE);
				gc.strokeLine(0, i * TILE, BOARD_SIZE, i * TILE);
			}
			gc.setFill(Color.web(NEON_CYAN));
			gc.setFont(Font.font("monospace", FontWeight.BOLD, 20));
			gc.fillText(">> AWAITING OPPONENT...", 60, BOARD_SIZE / 2.0);
			return;
		}

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				int dispRow = toDisplayRow(row);
				int dispCol = toDisplayCol(col);
				boolean isDark = (dispRow + dispCol) % 2 == 1;

				gc.setFill(isDark ? Color.web("#0d0d2b") : Color.web("#1a0a2e"));
				gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);

				gc.setStroke(Color.web(NEON_PURPLE, 0.25));
				gc.setLineWidth(0.5);
				gc.strokeRect(dispCol * TILE, dispRow * TILE, TILE, TILE);

				int piece = board[row][col];
				if (piece != CheckersConstants.EMPTY) {
					drawPiece(gc, dispRow, dispCol, piece);
				}

				if (pieceSelected && row == selectedRow && col == selectedCol) {
					gc.setFill(Color.web(NEON_YELLOW, 0.35));
					gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);
					gc.setStroke(Color.web(NEON_YELLOW));
					gc.setLineWidth(2);
					gc.strokeRect(dispCol * TILE + 1, dispRow * TILE + 1, TILE - 2, TILE - 2);
				}
			}
		}
	}

	private void drawPiece(GraphicsContext gc, int row, int col, int piece) {
		double x = col * TILE + TILE * 0.1;
		double y = row * TILE + TILE * 0.1;
		double size = TILE * 0.8;

		gc.setFill(Color.color(0, 0, 0, 0.5));
		gc.fillOval(x + 4, y + 4, size, size);

		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setFill(Color.web("#c0003a"));
			gc.fillOval(x, y, size, size);
			gc.setFill(Color.web(NEON_PINK, 0.6));
			gc.fillOval(x + size * 0.15, y + size * 0.1, size * 0.5, size * 0.3);
			gc.setStroke(Color.web(NEON_PINK));
		} else {
			gc.setFill(Color.web("#050520"));
			gc.fillOval(x, y, size, size);
			gc.setFill(Color.web(NEON_CYAN, 0.4));
			gc.fillOval(x + size * 0.15, y + size * 0.1, size * 0.5, size * 0.3);
			gc.setStroke(Color.web(NEON_CYAN));
		}

		gc.setLineWidth(2.5);
		gc.strokeOval(x + 2, y + 2, size - 4, size - 4);

		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setStroke(Color.web(NEON_PINK, 0.4));
		} else {
			gc.setStroke(Color.web(NEON_CYAN, 0.4));
		}
		gc.setLineWidth(1);
		gc.strokeOval(x + 8, y + 8, size - 16, size - 16);

		if (piece == CheckersConstants.RED_KING || piece == CheckersConstants.BLACK_KING) {
			gc.setFill(Color.web(NEON_YELLOW));
			gc.setFont(Font.font("monospace", FontWeight.BOLD, 18));
			gc.fillText("♛", col * TILE + TILE * 0.28, row * TILE + TILE * 0.67);
		}
	}

	private void updateTurnLabel() {
		if (currentTurn == myColor) {
			turnLabel.setText(">> YOUR TURN <<");
			turnLabel.setStyle("-fx-text-fill: " + NEON_GREEN + "; -fx-font-family: monospace; -fx-font-size: 15px; -fx-font-weight: bold;");
		} else {
			String otherName = (myColor == CheckersConstants.RED) ? blackPlayer : redPlayer;
			turnLabel.setText("// " + otherName.toUpperCase() + "'S TURN");
			turnLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-family: monospace; -fx-font-size: 14px;");
		}
	}

	private Button createMuteBtn(boolean isLobby) {
		Button btn = new Button(sound.isMuted() ? "🔇 MUTED" : "🔊 SOUND");
		btn.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: " + NEON_YELLOW +
				"; -fx-font-family: monospace; -fx-font-size: 10px; -fx-border-color: " +
				NEON_YELLOW + "; -fx-border-width: 1px;");
		btn.setOnAction(e -> applyMuteToggle());
		if (isLobby) lobbyMuteBtn = btn;
		else gameMuteBtn = btn;
		return btn;
	}

	private void applyMuteToggle() {
		sound.toggleMute();
		String label = sound.isMuted() ? "🔇 MUTED" : "🔊 SOUND";
		if (lobbyMuteBtn != null) lobbyMuteBtn.setText(label);
		if (gameMuteBtn != null) gameMuteBtn.setText(label);
	}

	private int toDisplayRow(int actualRow) { return myColor == CheckersConstants.BLACK ? 7 - actualRow : actualRow; }
	private int toDisplayCol(int actualCol) { return myColor == CheckersConstants.BLACK ? 7 - actualCol : actualCol; }
	private int toActualRow(int displayRow) { return myColor == CheckersConstants.BLACK ? 7 - displayRow : displayRow; }
	private int toActualCol(int displayCol) { return myColor == CheckersConstants.BLACK ? 7 - displayCol : displayCol; }

	// ─────────────────────────────────────────────
	//  GAME GUI
	// ─────────────────────────────────────────────

	public Scene createClientGui() {
		boardCanvas = new Canvas(BOARD_SIZE, BOARD_SIZE);
		boardCanvas.setOnMouseClicked(e -> handleBoardClick(e.getX(), e.getY()));
		drawBoard();

		statusLabel = new Label("> CONNECTING...");
		statusLabel.setStyle("-fx-text-fill: " + NEON_CYAN + "; -fx-font-family: monospace; -fx-font-size: 12px;");
		statusLabel.setWrapText(true);

		turnLabel = new Label("");
		turnLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");

		VBox bottomBar = new VBox(4, turnLabel, statusLabel);
		bottomBar.setAlignment(Pos.CENTER);
		bottomBar.setPadding(new Insets(8));
		bottomBar.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 1 0 0 0;");

		Label chatTitle = new Label("// COMMS //");
		chatTitle.setStyle(glowLabel(NEON_CYAN) + "-fx-font-size: 12px;");

		chatList = new ListView<>();
		chatList.setPrefWidth(220);
		chatList.setStyle("-fx-background-color: " + BG_DARK + "; -fx-control-inner-background: " + BG_DARK + "; -fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 1px;");
		chatList.setCellFactory(lv -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) { setText(null); setStyle(""); }
				else {
					setText(item);
					setStyle("-fx-text-fill: " + NEON_GREEN + "; -fx-font-family: monospace; -fx-font-size: 11px; -fx-background-color: " + BG_DARK + ";");
				}
			}
		});
		VBox.setVgrow(chatList, Priority.ALWAYS);

		chatInput = new TextField();
		chatInput.setPromptText("transmit...");
		chatInput.setStyle("-fx-background-color: #0d0d1a; -fx-text-fill: " + NEON_GREEN + "; -fx-prompt-text-fill: #334433; -fx-font-family: monospace; -fx-font-size: 11px; -fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 1px;");

		chatSendBtn = new Button("TX");
		chatSendBtn.setStyle(neonBtn(NEON_CYAN));
		chatSendBtn.setDisable(true);

		Runnable sendChat = () -> {
			String text = chatInput.getText().trim();
			if (text.isEmpty() || username == null) return;
			chatList.getItems().add("[YOU] " + text);
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
		chatPanel.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 0 0 0 1;");
		chatPanel.setPrefWidth(240);

		Label gameTag = new Label("[ CHECKERS ARCADE ]");
		gameTag.setStyle(glowLabel(NEON_PURPLE) + "-fx-font-size: 10px;");

		toggleChat = new Button("💬 COMMS ▶");
		toggleChat.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: " + NEON_CYAN + "; -fx-font-family: monospace; -fx-font-size: 10px; -fx-border-color: " + NEON_CYAN + "; -fx-border-width: 1px;");
		toggleChat.setOnAction(e -> {
			boolean visible = chatPanel.isVisible();
			chatPanel.setVisible(!visible);
			chatPanel.setManaged(!visible);
			toggleChat.setText(!visible ? "💬 COMMS ◀" : "💬 COMMS ▶");
			toggleChat.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: " + NEON_CYAN + "; -fx-font-family: monospace; -fx-font-size: 10px; -fx-border-color: " + NEON_CYAN + "; -fx-border-width: 1px;");
			Stage stage = (Stage) primaryStage.getScene().getWindow();
			stage.setWidth(visible ? BOARD_SIZE + 40 : BOARD_SIZE + 280);
		});

		Button gameMuteBtn = createMuteBtn(false);

		HBox topBar = new HBox(gameTag, gameMuteBtn, toggleChat);
		topBar.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(gameTag, Priority.ALWAYS);
		topBar.setPadding(new Insets(6, 10, 6, 10));
		topBar.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: " + NEON_PURPLE + "; -fx-border-width: 0 0 1 0;");

		BorderPane root = new BorderPane();
		root.setTop(topBar);
		root.setCenter(boardCanvas);
		root.setBottom(bottomBar);
		root.setRight(chatPanel);
		root.setStyle("-fx-background-color: " + BG_DARK + ";");

		return new Scene(root, BOARD_SIZE + 40, BOARD_SIZE + 80);
	}
}
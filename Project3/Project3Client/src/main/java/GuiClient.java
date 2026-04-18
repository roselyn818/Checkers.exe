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

public class GuiClient extends Application {

	HashMap<String, Scene> sceneMap;
	Client clientConnection;

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

	static final int TILE = 75;
	static final int BOARD_SIZE = 8 * TILE;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		clientConnection = new Client(data -> {
			Platform.runLater(() -> handleIncoming((Message) data));
		});
		clientConnection.start();

		sceneMap = new HashMap<>();
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Checkers");
		primaryStage.show();

		Platform.runLater(() -> askForUsername(primaryStage));
	}

	private void askForUsername(Stage owner) {
		askForUsername(owner, null);
	}

	private void askForUsername(Stage owner, String errorMsg) {
		Stage dialog = new Stage();
		dialog.setTitle("Join Checkers");
		dialog.initOwner(owner);

		TextField nameField = new TextField();
		nameField.setPromptText("Enter username");
		Button okBtn = new Button("Join Game");
		Label errLabel = new Label(errorMsg != null ? errorMsg : "");
		errLabel.setStyle("-fx-text-fill: red;");

		okBtn.setOnAction(e -> {
			String name = nameField.getText().trim();
			if (name.isEmpty()) {
				errLabel.setText("Username cannot be empty.");
				return;
			}
			okBtn.setDisable(true);
			clientConnection.send(Message.setUsername(name));
			dialog.setUserData("pending");
		});

		nameField.setOnAction(e -> okBtn.fire());

		VBox box = new VBox(10,
				new Label("Enter your username to join the game:"),
				nameField, okBtn, errLabel);
		box.setPadding(new Insets(20));

		dialog.setScene(new Scene(box, 300, 160));
		owner.setUserData(dialog);
		dialog.show();
	}

	private void handleIncoming(Message msg) {
		switch (msg.getType()) {

			case username_accepted: {
				username = msg.getContent();
				Stage owner = (Stage) sceneMap.get("client").getWindow();
				Object ud = owner.getUserData();
				if (ud instanceof Stage) ((Stage) ud).close();
				owner.setTitle("Checkers — " + username);
				statusLabel.setText("Connected as " + username + ". Joining game...");
				clientConnection.send(Message.joinGame(username));
				break;
			}

			case username_taken: {
				Stage owner = (Stage) sceneMap.get("client").getWindow();
				Object ud = owner.getUserData();
				if (ud instanceof Stage) ((Stage) ud).close();
				askForUsername(owner, msg.getContent());
				break;
			}

			case waiting_for_opponent: {
				statusLabel.setText("Waiting for another player to join...");
				break;
			}

			case game_start: {
				redPlayer = msg.getRedPlayer();
				blackPlayer = msg.getBlackPlayer();
				board = msg.getBoard();
				currentTurn = CheckersConstants.RED;
				myColor = username.equals(redPlayer) ? CheckersConstants.RED : CheckersConstants.BLACK;
				statusLabel.setText("Game started! You are " + (myColor == CheckersConstants.RED ? "RED" : "BLACK"));
				updateTurnLabel();
				drawBoard();
				chatSendBtn.setDisable(false);
				break;
			}

			case game_state: {
				board = msg.getBoard();
				currentTurn = msg.getCurrentTurn();
				int mjRow = msg.getMultiJumpRow();
				int mjCol = msg.getMultiJumpCol();

				if (currentTurn == myColor && mjRow != -1) {
					// Lock onto the jumping piece
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
				String winner = msg.getWinner();
				statusLabel.setText("Game Over! " + msg.getContent());
				turnLabel.setText("");

				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Game Over");
				alert.setHeaderText(null);
				alert.setContentText(msg.getContent());
				alert.show();
				break;
			}

			case chat: {
				chatList.getItems().add(msg.getSenderUsername() + ": " + msg.getContent());
				chatList.scrollTo(chatList.getItems().size() - 1);
				// Show unread badge if chat is collapsed
				if (!chatPanel.isVisible()) {
					String current = toggleChat.getText();
					// Parse existing unread count if present
					int unread = 1;
					if (current.contains("(")) {
						try {
							unread = Integer.parseInt(current.replaceAll(".*\\((\\d+)\\).*", "$1")) + 1;
						} catch (NumberFormatException ignored) {}
					}
					toggleChat.setText("💬 Chat ▶ (" + unread + ")");
					toggleChat.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px;");
				}
				break;
			}

			default:
				statusLabel.setText("Unknown message: " + msg);
		}
	}

	private void handleBoardClick(double x, double y) {
		if (board == null) return;
		if (myColor != currentTurn) {
			statusLabel.setText("It's not your turn!");
			return;
		}

		// Display grid position from pixel
		int dispCol = (int) (x / TILE);
		int dispRow = (int) (y / TILE);
		if (dispRow < 0 || dispRow > 7 || dispCol < 0 || dispCol > 7) return;

		// Convert display position → actual board position
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
				// Don't allow deselecting mid multi-jump
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
			// If multiJumpActive, leave the piece selected — server response will update state
		}
	}

	private int getKingColor(int color) {
		if (color == CheckersConstants.RED) return CheckersConstants.RED_KING;
		if (color == CheckersConstants.BLACK) return CheckersConstants.BLACK_KING;
		return CheckersConstants.EMPTY;
	}

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
				// Convert actual board position → display position
				int dispRow = toDisplayRow(row);
				int dispCol = toDisplayCol(col);

				// Tile color (based on display position)
				boolean isDark = (dispRow + dispCol) % 2 == 1;
				gc.setFill(isDark ? Color.SADDLEBROWN : Color.WHEAT);
				gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);

				// Highlight selected piece
				if (pieceSelected && row == selectedRow && col == selectedCol) {
					gc.setFill(Color.color(1, 1, 0, 0.5));
					gc.fillRect(dispCol * TILE, dispRow * TILE, TILE, TILE);
				}

				// Draw piece at display position
				int piece = board[row][col];
				if (piece != CheckersConstants.EMPTY) {
					drawPiece(gc, dispRow, dispCol, piece);
				}
			}
		}
	}

	private void drawPiece(GraphicsContext gc, int row, int col, int piece) {
		double x = col * TILE + TILE * 0.1;
		double y = row * TILE + TILE * 0.1;
		double size = TILE * 0.8;

		// Shadow
		gc.setFill(Color.color(0, 0, 0, 0.3));
		gc.fillOval(x + 3, y + 3, size, size);

		// Piece color
		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setFill(Color.CRIMSON);
		} else {
			gc.setFill(Color.color(0.1, 0.1, 0.1));
		}
		gc.fillOval(x, y, size, size);

		// Highlight ring
		if (piece == CheckersConstants.RED || piece == CheckersConstants.RED_KING) {
			gc.setStroke(Color.LIGHTCORAL);
		} else {
			gc.setStroke(Color.DIMGRAY);
		}
		gc.setLineWidth(2);
		gc.strokeOval(x + 4, y + 4, size - 8, size - 8);

		// Crown for king
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

	// Convert actual board row/col → display row/col (flipped for BLACK)
	private int toDisplayRow(int actualRow) {
		return myColor == CheckersConstants.BLACK ? 7 - actualRow : actualRow;
	}
	private int toDisplayCol(int actualCol) {
		return myColor == CheckersConstants.BLACK ? 7 - actualCol : actualCol;
	}

	// Convert clicked display row/col → actual board row/col
	private int toActualRow(int displayRow) {
		return myColor == CheckersConstants.BLACK ? 7 - displayRow : displayRow;
	}
	private int toActualCol(int displayCol) {
		return myColor == CheckersConstants.BLACK ? 7 - displayCol : displayCol;
	}

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

		// --- Chat panel ---
		Label chatTitle = new Label("Chat");
		chatTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

		chatList = new ListView<>();
		chatList.setPrefWidth(220);
		chatList.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-text-fill: white;");
		VBox.setVgrow(chatList, javafx.scene.layout.Priority.ALWAYS);

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
			// Show own message locally
			chatList.getItems().add("You: " + text);
			chatList.scrollTo(chatList.getItems().size() - 1);
			clientConnection.send(Message.chat(username, text));
			chatInput.clear();
		};

		chatSendBtn.setOnAction(e -> sendChat.run());
		chatInput.setOnAction(e -> sendChat.run());

		HBox chatInputRow = new HBox(6, chatInput, chatSendBtn);
		HBox.setHgrow(chatInput, javafx.scene.layout.Priority.ALWAYS);
		chatInputRow.setPadding(new Insets(4, 0, 0, 0));

		chatPanel = new VBox(8, chatTitle, chatList, chatInputRow);
		chatPanel.setVisible(false);
		chatPanel.setManaged(false);
		chatPanel.setPadding(new Insets(10));
		chatPanel.setStyle("-fx-background-color: #1e1e1e;");
		chatPanel.setPrefWidth(240);

		// Enable send button once game starts
		// (done in handleIncoming game_start case below)

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
				// Panel is opening — clear badge
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
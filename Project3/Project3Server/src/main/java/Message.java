import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        // Connection
        set_username,
        username_accepted,
        username_taken,

        // Game flow
        join_game,
        waiting_for_opponent,
        game_start,
        make_move,
        game_state,
        invalid_move,
        game_over
    }

    private MessageType type;
    private String senderUsername;
    private String content;

    // Move fields
    private int fromRow, fromCol, toRow, toCol;

    // Board state
    private int[][] board;
    private int currentTurn; // CheckersGame.RED or BLACK
    private String redPlayer;
    private String blackPlayer;
    private String winner;

    public Message(MessageType type) {
        this.type = type;
    }

    // ---- Factory methods ----

    public static Message setUsername(String username) {
        Message m = new Message(MessageType.set_username);
        m.senderUsername = username;
        return m;
    }

    public static Message usernameAccepted(String username) {
        Message m = new Message(MessageType.username_accepted);
        m.content = username;
        return m;
    }

    public static Message usernameTaken(String username) {
        Message m = new Message(MessageType.username_taken);
        m.content = username + " is already taken.";
        return m;
    }

    public static Message joinGame(String username) {
        Message m = new Message(MessageType.join_game);
        m.senderUsername = username;
        return m;
    }

    public static Message waitingForOpponent() {
        Message m = new Message(MessageType.waiting_for_opponent);
        m.content = "Waiting for another player to join...";
        return m;
    }

    public static Message gameStart(String redPlayer, String blackPlayer, int[][] board) {
        Message m = new Message(MessageType.game_start);
        m.redPlayer = redPlayer;
        m.blackPlayer = blackPlayer;
        m.board = board;
        m.currentTurn = 1; // RED = 1
        m.content = "Game started! RED: " + redPlayer + " | BLACK: " + blackPlayer;
        return m;
    }

    public static Message makeMove(String username, int fromRow, int fromCol, int toRow, int toCol) {
        Message m = new Message(MessageType.make_move);
        m.senderUsername = username;
        m.fromRow = fromRow;
        m.fromCol = fromCol;
        m.toRow = toRow;
        m.toCol = toCol;
        return m;
    }

    public static Message gameState(int[][] board, int currentTurn, String redPlayer, String blackPlayer) {
        Message m = new Message(MessageType.game_state);
        m.board = board;
        m.currentTurn = currentTurn;
        m.redPlayer = redPlayer;
        m.blackPlayer = blackPlayer;
        return m;
    }

    public static Message invalidMove(String reason) {
        Message m = new Message(MessageType.invalid_move);
        m.content = reason;
        return m;
    }

    public static Message gameOver(String winner) {
        Message m = new Message(MessageType.game_over);
        m.winner = winner;
        m.content = winner + " wins!";
        return m;
    }

    // ---- Getters ----

    public MessageType getType() { return type; }
    public String getSenderUsername() { return senderUsername; }
    public String getContent() { return content; }
    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }
    public int[][] getBoard() { return board; }
    public int getCurrentTurn() { return currentTurn; }
    public String getRedPlayer() { return redPlayer; }
    public String getBlackPlayer() { return blackPlayer; }
    public String getWinner() { return winner; }

    @Override
    public String toString() {
        return "[Message type=" + type + " sender=" + senderUsername + " content=" + content + "]";
    }
}
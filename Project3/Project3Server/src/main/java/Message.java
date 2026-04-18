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
        chat,

        // Lobby
        lobby_update,
        challenge_send,
        challenge_receive,
        challenge_accept,
        challenge_decline,
        challenge_declined,

        // Game flow
        join_game,
        waiting_for_opponent,
        game_start,
        make_move,
        game_state,
        invalid_move,
        game_over,

        // Rematch
        rematch_request,
        rematch_offer,
        rematch_accept,
        rematch_decline
    }

    private MessageType type;
    private String senderUsername;
    private String recipientUsername;
    private String content;

    // Move fields
    private int fromRow, fromCol, toRow, toCol;

    // Board state
    private int[][] board;
    private int currentTurn;
    private String redPlayer;
    private String blackPlayer;
    private String winner;

    // Multi-jump
    private int multiJumpRow = -1;
    private int multiJumpCol = -1;

    // Lobby
    private List<String> playerList;

    public Message(MessageType type) {
        this.type = type;
        this.playerList = new ArrayList<>();
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

    public static Message lobbyUpdate(List<String> players) {
        Message m = new Message(MessageType.lobby_update);
        m.playerList = new ArrayList<>(players);
        return m;
    }

    public static Message challengeSend(String from, String to) {
        Message m = new Message(MessageType.challenge_send);
        m.senderUsername = from;
        m.recipientUsername = to;
        return m;
    }

    public static Message challengeReceive(String from) {
        Message m = new Message(MessageType.challenge_receive);
        m.senderUsername = from;
        m.content = from + " challenged you!";
        return m;
    }

    public static Message challengeAccept(String from, String to) {
        Message m = new Message(MessageType.challenge_accept);
        m.senderUsername = from;
        m.recipientUsername = to;
        return m;
    }

    public static Message challengeDecline(String from, String to) {
        Message m = new Message(MessageType.challenge_decline);
        m.senderUsername = from;
        m.recipientUsername = to;
        return m;
    }

    public static Message challengeDeclined(String decliner) {
        Message m = new Message(MessageType.challenge_declined);
        m.content = decliner + " declined your challenge.";
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

    public static Message gameState(int[][] board, int currentTurn, String redPlayer, String blackPlayer, int multiJumpRow, int multiJumpCol) {
        Message m = new Message(MessageType.game_state);
        m.board = board;
        m.currentTurn = currentTurn;
        m.redPlayer = redPlayer;
        m.blackPlayer = blackPlayer;
        m.multiJumpRow = multiJumpRow;
        m.multiJumpCol = multiJumpCol;
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

    public static Message gameOverDisconnect(String winnerUsername) {
        Message m = new Message(MessageType.game_over);
        m.winner = winnerUsername;
        m.content = "Opponent disconnected. " + winnerUsername + " wins!";
        return m;
    }

    public static Message chat(String username, String text) {
        Message m = new Message(MessageType.chat);
        m.senderUsername = username;
        m.content = text;
        return m;
    }

    public static Message rematchRequest(String username) {
        Message m = new Message(MessageType.rematch_request);
        m.senderUsername = username;
        return m;
    }

    public static Message rematchOffer(String from) {
        Message m = new Message(MessageType.rematch_offer);
        m.senderUsername = from;
        m.content = from + " wants a rematch!";
        return m;
    }

    public static Message rematchAccept(String username) {
        Message m = new Message(MessageType.rematch_accept);
        m.senderUsername = username;
        return m;
    }

    public static Message rematchDecline(String username) {
        Message m = new Message(MessageType.rematch_decline);
        m.senderUsername = username;
        return m;
    }

    // ---- Getters ----

    public MessageType getType() { return type; }
    public String getSenderUsername() { return senderUsername; }
    public String getRecipientUsername() { return recipientUsername; }
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
    public int getMultiJumpRow() { return multiJumpRow; }
    public int getMultiJumpCol() { return multiJumpCol; }
    public List<String> getPlayerList() { return playerList; }

    @Override
    public String toString() {
        return "[Message type=" + type + " sender=" + senderUsername + " recipient=" + recipientUsername + " content=" + content + "]";
    }
}
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CheckersGame implements Serializable {
    static final long serialVersionUID = 99L;

    public static final int EMPTY = 0;
    public static final int RED = 1;
    public static final int RED_KING = 2;
    public static final int BLACK = 3;
    public static final int BLACK_KING = 4;

    private int[][] board;
    private int currentTurn; // RED or BLACK
    private String redPlayer;
    private String blackPlayer;
    private boolean gameOver;
    private String winner;

    public CheckersGame(String redPlayer, String blackPlayer) {
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.currentTurn = RED; // RED goes first
        this.gameOver = false;
        this.winner = null;
        board = new int[8][8];
        initBoard();
    }

    private void initBoard() {
        // Place BLACK pieces on rows 0-2
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = BLACK;
                }
            }
        }
        // Place RED pieces on rows 5-7
        for (int row = 5; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = RED;
                }
            }
        }
    }

    // Returns true if the move was valid and applied
    public boolean makeMove(String username, int fromRow, int fromCol, int toRow, int toCol) {
        if (gameOver) return false;

        int playerColor = getPlayerColor(username);
        if (playerColor == EMPTY) return false;
        if (playerColor != currentTurn) return false;

        int piece = board[fromRow][fromCol];
        if (piece == EMPTY) return false;
        if (piece != playerColor && piece != getKingColor(playerColor)) return false;

        // Check if it's a valid move
        if (!isValidMove(fromRow, fromCol, toRow, toCol, playerColor)) return false;

        // Apply the move
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = EMPTY;

        // Remove jumped piece if it's a capture
        if (Math.abs(toRow - fromRow) == 2) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;
            board[midRow][midCol] = EMPTY;
        }

        // King promotion
        if (piece == RED && toRow == 0) board[toRow][toCol] = RED_KING;
        if (piece == BLACK && toRow == 7) board[toRow][toCol] = BLACK_KING;

        // Check for win
        checkWin();

        // Switch turns
        if (!gameOver) {
            currentTurn = (currentTurn == RED) ? BLACK : RED;
        }

        return true;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, int playerColor) {
        // Must be in bounds
        if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) return false;
        // Destination must be empty
        if (board[toRow][toCol] != EMPTY) return false;

        int piece = board[fromRow][fromCol];
        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);

        // Must move diagonally
        if (colDiff != 1 && colDiff != 2) return false;

        // Direction check (non-kings)
        if (piece == RED && rowDiff >= 0) return false;       // RED moves up (decreasing row)
        if (piece == BLACK && rowDiff <= 0) return false;     // BLACK moves down (increasing row)

        // Normal move (1 step)
        if (Math.abs(rowDiff) == 1 && colDiff == 1) return true;

        // Jump move (2 steps)
        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;
            int midPiece = board[midRow][midCol];
            // Must jump over an opponent piece
            if (playerColor == RED && (midPiece == BLACK || midPiece == BLACK_KING)) return true;
            if (playerColor == BLACK && (midPiece == RED || midPiece == RED_KING)) return true;
        }

        return false;
    }

    private void checkWin() {
        boolean redExists = false;
        boolean blackExists = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (board[row][col] == RED || board[row][col] == RED_KING) redExists = true;
                if (board[row][col] == BLACK || board[row][col] == BLACK_KING) blackExists = true;
            }
        }

        if (!redExists) {
            gameOver = true;
            winner = blackPlayer;
        } else if (!blackExists) {
            gameOver = true;
            winner = redPlayer;
        }
    }

    private int getPlayerColor(String username) {
        if (username.equals(redPlayer)) return RED;
        if (username.equals(blackPlayer)) return BLACK;
        return EMPTY;
    }

    private int getKingColor(int color) {
        if (color == RED) return RED_KING;
        if (color == BLACK) return BLACK_KING;
        return EMPTY;
    }

    // Returns a list of valid moves for a player as int[] {fromRow, fromCol, toRow, toCol}
    public List<int[]> getValidMoves(String username) {
        List<int[]> moves = new ArrayList<>();
        int playerColor = getPlayerColor(username);
        if (playerColor == EMPTY) return moves;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == playerColor || piece == getKingColor(playerColor)) {
                    int[] directions = getDirections(piece);
                    for (int dr : directions) {
                        // Normal moves
                        for (int dc : new int[]{-1, 1}) {
                            if (isValidMove(row, col, row + dr, col + dc, playerColor)) {
                                moves.add(new int[]{row, col, row + dr, col + dc});
                            }
                            // Jump moves
                            if (isValidMove(row, col, row + dr * 2, col + dc * 2, playerColor)) {
                                moves.add(new int[]{row, col, row + dr * 2, col + dc * 2});
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    private int[] getDirections(int piece) {
        if (piece == RED) return new int[]{-1};
        if (piece == BLACK) return new int[]{1};
        return new int[]{-1, 1}; // Kings move both ways
    }

    // Getters
    public int[][] getBoard() {
        int[][] copy = new int[8][8];
        for (int r = 0; r < 8; r++)
            copy[r] = board[r].clone();
        return copy;
    }
    public int getCurrentTurn() { return currentTurn; }
    public String getRedPlayer() { return redPlayer; }
    public String getBlackPlayer() { return blackPlayer; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    public String getCurrentTurnUsername() {
        return (currentTurn == RED) ? redPlayer : blackPlayer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                sb.append(board[row][col]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
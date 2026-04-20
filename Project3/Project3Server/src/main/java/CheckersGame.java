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

    private int multiJumpRow = -1;
    private int multiJumpCol = -1;

    public CheckersGame(String redPlayer, String blackPlayer) {
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.currentTurn = RED;
        this.gameOver = false;
        this.winner = null;
        board = new int[8][8];
        initBoard();
    }

    private void initBoard() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) board[row][col] = BLACK;
            }
        }
        for (int row = 5; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) board[row][col] = RED;
            }
        }
    }

    public boolean makeMove(String username, int fromRow, int fromCol, int toRow, int toCol) {
        if (gameOver) return false;

        if (multiJumpRow != -1) {
            if (fromRow != multiJumpRow || fromCol != multiJumpCol) return false;
            if (Math.abs(toRow - fromRow) != 2) return false;
        }

        int playerColor = getPlayerColor(username);
        if (playerColor == EMPTY) return false;
        if (playerColor != currentTurn) return false;

        int piece = board[fromRow][fromCol];
        if (piece == EMPTY) return false;
        if (piece != playerColor && piece != getKingColor(playerColor)) return false;

        if (!isValidMove(fromRow, fromCol, toRow, toCol, playerColor)) return false;

        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = EMPTY;

        boolean wasCapture = Math.abs(toRow - fromRow) == 2;

        if (wasCapture) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;
            board[midRow][midCol] = EMPTY;
        }

        // King promotion
        if (piece == RED && toRow == 0) board[toRow][toCol] = RED_KING;
        if (piece == BLACK && toRow == 7) board[toRow][toCol] = BLACK_KING;

        // Check if all pieces captured
        checkWin();

        if (!gameOver) {
            if (wasCapture && canCaptureFrom(toRow, toCol, playerColor)) {
                multiJumpRow = toRow;
                multiJumpCol = toCol;
            } else {
                multiJumpRow = -1;
                multiJumpCol = -1;
                // Switch turn
                currentTurn = (currentTurn == RED) ? BLACK : RED;
                // Check if the player whose turn it now is has ANY valid move
                checkNoMoves();
            }
        }

        return true;
    }

    // Checks if the current player (after turn switch) has any moves at all
    private void checkNoMoves() {
        if (gameOver) return;
        boolean currentHasMoves = hasAnyMove(currentTurn);
        if (!currentHasMoves) {
            // Check if the OTHER player also has no moves → draw
            int otherColor = (currentTurn == RED) ? BLACK : RED;
            boolean otherHasMoves = hasAnyMove(otherColor);
            gameOver = true;
            if (!otherHasMoves) {
                winner = null; // draw
            } else {
                winner = (currentTurn == RED) ? blackPlayer : redPlayer;
            }
        }
    }

    // Returns true if the given color has at least one valid move anywhere on the board
    public boolean hasAnyMove(int playerColor) {
        System.out.println("=== hasAnyMove called for playerColor=" + playerColor + " ===");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece != playerColor && piece != getKingColor(playerColor)) continue;
                int[] dirs = getDirections(piece);
                for (int dr : dirs) {
                    for (int dc : new int[]{-1, 1}) {
                        if (isValidMove(row, col, row + dr, col + dc, playerColor)) {
                            return true;
                        }
                        if (isValidMove(row, col, row + dr * 2, col + dc * 2, playerColor)) {
                            return true;
                        }
                    }
                }
            }
        }
        System.out.println("  NO MOVES FOUND for playerColor=" + playerColor);
        return false;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, int playerColor) {
        if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) return false;
        if (board[toRow][toCol] != EMPTY) return false;

        int piece = board[fromRow][fromCol];
        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);

        if (colDiff != 1 && colDiff != 2) return false;

        // Direction check for non-kings only
        if (piece == RED && rowDiff >= 0) return false;
        if (piece == BLACK && rowDiff <= 0) return false;
        // Kings (RED_KING, BLACK_KING) can move in any diagonal direction — no direction check needed

        if (Math.abs(rowDiff) == 1 && colDiff == 1) return true;

        if (Math.abs(rowDiff) == 2 && colDiff == 2) {
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;
            int midPiece = board[midRow][midCol];
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

        if (!redExists) { gameOver = true; winner = blackPlayer; }
        else if (!blackExists) { gameOver = true; winner = redPlayer; }
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
                        for (int dc : new int[]{-1, 1}) {
                            if (isValidMove(row, col, row + dr, col + dc, playerColor))
                                moves.add(new int[]{row, col, row + dr, col + dc});
                            if (isValidMove(row, col, row + dr * 2, col + dc * 2, playerColor))
                                moves.add(new int[]{row, col, row + dr * 2, col + dc * 2});
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

    private boolean canCaptureFrom(int row, int col, int playerColor) {
        int piece = board[row][col];
        int[] directions = getDirections(piece);
        for (int dr : directions) {
            for (int dc : new int[]{-1, 1}) {
                if (isValidMove(row, col, row + dr * 2, col + dc * 2, playerColor)) return true;
            }
        }
        return false;
    }

    public int[][] getBoard() {
        int[][] copy = new int[8][8];
        for (int r = 0; r < 8; r++) copy[r] = board[r].clone();
        return copy;
    }

    public int getCurrentTurn() { return currentTurn; }
    public String getRedPlayer() { return redPlayer; }
    public String getBlackPlayer() { return blackPlayer; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
    public int getMultiJumpRow() { return multiJumpRow; }
    public int getMultiJumpCol() { return multiJumpCol; }

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
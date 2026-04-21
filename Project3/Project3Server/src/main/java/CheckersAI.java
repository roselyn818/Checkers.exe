import java.util.List;
import java.util.ArrayList;

public class CheckersAI {

    public enum Difficulty {
        EASY(2), MEDIUM(4), HARD(6);
        final int depth;
        Difficulty(int depth) { this.depth = depth; }
    }

    private final Difficulty difficulty;

    public CheckersAI(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() { return difficulty; }

    // Returns best move as [fromRow, fromCol, toRow, toCol], or null if no moves.
    public int[] getBestMove(int[][] board, int color) {
        List<int[]> moves = getAllMoves(board, color);
        if (moves.isEmpty()) return null;

        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = moves.get(0);

        for (int[] move : moves) {
            int[][] next = applyMove(board, move);
            int score = minimax(next, difficulty.depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, color);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(int[][] board, int depth, int alpha, int beta, boolean maximizing, int aiColor) {
        int opponent = (aiColor == CheckersGame.BLACK) ? CheckersGame.RED : CheckersGame.BLACK;
        int currentColor = maximizing ? aiColor : opponent;
        List<int[]> moves = getAllMoves(board, currentColor);

        if (depth == 0 || moves.isEmpty()) return evaluate(board, aiColor);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] move : moves) {
                int[][] next = applyMove(board, move);
                if (Math.abs(move[2] - move[0]) == 2) {
                    List<int[]> fus = getCapturesFrom(next, move[2], move[3], aiColor);
                    if (!fus.isEmpty()) {
                        for (int[] fu : fus) {
                            int eval = minimax(applyMove(next, fu), depth - 1, alpha, beta, true, aiColor);
                            maxEval = Math.max(maxEval, eval);
                            alpha = Math.max(alpha, eval);
                            if (beta <= alpha) break;
                        }
                        continue;
                    }
                }
                int eval = minimax(next, depth - 1, alpha, beta, false, aiColor);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int[] move : moves) {
                int[][] next = applyMove(board, move);
                if (Math.abs(move[2] - move[0]) == 2) {
                    List<int[]> fus = getCapturesFrom(next, move[2], move[3], opponent);
                    if (!fus.isEmpty()) {
                        for (int[] fu : fus) {
                            int eval = minimax(applyMove(next, fu), depth - 1, alpha, beta, false, aiColor);
                            minEval = Math.min(minEval, eval);
                            beta = Math.min(beta, eval);
                            if (beta <= alpha) break;
                        }
                        continue;
                    }
                }
                int eval = minimax(next, depth - 1, alpha, beta, true, aiColor);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluate(int[][] board, int aiColor) {
        int opponent = (aiColor == CheckersGame.BLACK) ? CheckersGame.RED  : CheckersGame.BLACK;
        int aiKing   = (aiColor == CheckersGame.BLACK) ? CheckersGame.BLACK_KING : CheckersGame.RED_KING;
        int oppKing  = (aiColor == CheckersGame.BLACK) ? CheckersGame.RED_KING   : CheckersGame.BLACK_KING;
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if      (piece == aiColor)   { score += 10; score += (aiColor == CheckersGame.BLACK) ? row : (7 - row); if (col >= 2 && col <= 5) score += 2; }
                else if (piece == aiKing)    { score += 23; }
                else if (piece == opponent)  { score -= 10; }
                else if (piece == oppKing)   { score -= 23; }
            }
        }
        return score;
    }

    private List<int[]> getAllMoves(int[][] board, int color) {
        List<int[]> captures = new ArrayList<>();
        List<int[]> regular  = new ArrayList<>();
        int king = (color == CheckersGame.BLACK) ? CheckersGame.BLACK_KING : CheckersGame.RED_KING;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece != color && piece != king) continue;
                for (int dr : directions(piece)) {
                    for (int dc : new int[]{-1, 1}) {
                        if (isOnBoard(row+dr, col+dc) && board[row+dr][col+dc] == CheckersGame.EMPTY)
                            regular.add(new int[]{row, col, row+dr, col+dc});
                        int mr = row+dr, mc = col+dc, lr = row+dr*2, lc = col+dc*2;
                        if (isOnBoard(lr, lc) && board[lr][lc] == CheckersGame.EMPTY && isOpponent(board[mr][mc], color))
                            captures.add(new int[]{row, col, lr, lc});
                    }
                }
            }
        }
        return captures.isEmpty() ? regular : captures;
    }

    private List<int[]> getCapturesFrom(int[][] board, int row, int col, int color) {
        List<int[]> caps = new ArrayList<>();
        int piece = board[row][col];
        if (piece == CheckersGame.EMPTY) return caps;
        for (int dr : directions(piece)) {
            for (int dc : new int[]{-1, 1}) {
                int mr = row+dr, mc = col+dc, lr = row+dr*2, lc = col+dc*2;
                if (isOnBoard(lr, lc) && board[lr][lc] == CheckersGame.EMPTY && isOpponent(board[mr][mc], color))
                    caps.add(new int[]{row, col, lr, lc});
            }
        }
        return caps;
    }

    private int[][] applyMove(int[][] board, int[] move) {
        int[][] next = new int[8][8];
        for (int r = 0; r < 8; r++) next[r] = board[r].clone();
        int piece = next[move[0]][move[1]];
        next[move[2]][move[3]] = piece;
        next[move[0]][move[1]] = CheckersGame.EMPTY;
        if (Math.abs(move[2] - move[0]) == 2)
            next[(move[0]+move[2])/2][(move[1]+move[3])/2] = CheckersGame.EMPTY;
        if (piece == CheckersGame.BLACK && move[2] == 7) next[move[2]][move[3]] = CheckersGame.BLACK_KING;
        if (piece == CheckersGame.RED   && move[2] == 0) next[move[2]][move[3]] = CheckersGame.RED_KING;
        return next;
    }

    private int[] directions(int piece) {
        if (piece == CheckersGame.RED)   return new int[]{-1};
        if (piece == CheckersGame.BLACK) return new int[]{1};
        return new int[]{-1, 1};
    }

    private boolean isOnBoard(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    private boolean isOpponent(int piece, int myColor) {
        if (myColor == CheckersGame.BLACK) return piece == CheckersGame.RED || piece == CheckersGame.RED_KING;
        else return piece == CheckersGame.BLACK || piece == CheckersGame.BLACK_KING;
    }
}
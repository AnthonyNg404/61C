/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author  Xinyu
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {

        for (int row = 0; row < contents.length; row++) {
            for (int col = 0; col < contents[0].length; col++) {
                Square sq =  sq(col, row);
                set(sq, contents[row][col], side);
            }
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        _winnerKnown = false;
        _moves.clear();
        _winner = null;
        _subsetsInitialized = false;



        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;



    }

    /** Set me to the initial configuration. */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(Board board) {

        if (board == this) {
            return;
        }
        this._turn = board.turn();

        this._moves.addAll(board._moves);

        for (int j = 0; j < board._board.length; j++) {
            this._board[j] = board._board[j];
        }
        this._moveLimit = board._moveLimit;
        this._winner = board._winner;
        this._subsetsInitialized = board._subsetsInitialized;
        this._winnerKnown = board._winnerKnown;

    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        _board[sq.index()] = v;
        if (next != null) {
            _turn = next;
        }
    }
    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }
    /** Set limit on number of moves (before tie results) to LIMIT. */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }

    }
    /** change the turn. */


    void changeTurn() {
        if (turn() == WP) {
            _turn = BP;
        } else {
            _turn = WP;
        }
    }
    /** Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture()
     *  is false. If it saves the move for
     *  *  later retraction, makeMove itself uses MOVE.captureMove() to produce
     *  *  the capturing move. */

    void makeMove(Move move) {

        assert isLegal(move);
        if (_board[move.getTo().index()] == EMP) {
            _moves.add(move);
        } else {
            _moves.add(move.captureMove());
        }
        _board[move.getTo().index()] = _board[move.getFrom().index()];
        _board[move.getFrom().index()] = EMP;
        changeTurn();
        _subsetsInitialized = false;
        _winnerKnown = false;
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        assert movesMade() > 0;

        Move lastMove = _moves.remove(_moves.size() - 1);
        if (lastMove.isCapture()) {
            _board[lastMove.getFrom().index()]
                    = _board[lastMove.getTo().index()];
            if (_board[lastMove.getTo().index()] == BP) {
                _board[lastMove.getTo().index()] = WP;
            } else {
                _board[lastMove.getTo().index()] = BP;
            }
        } else {
            _board[lastMove.getFrom().index()]
                    = _board[lastMove.getTo().index()];
            _board[lastMove.getTo().index()] = EMP;
        }

        changeTurn();
        _subsetsInitialized = false;
        _winnerKnown = false;

    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }
    /** Return the number of pieces on a line.
     * @param dir  direction
     *  @param   position current position. */
    int numPiece(int dir, Square position) {
        int oppositeDir = (dir + 4) % 8;
        int totalPiece = 0;
        int i = 1;
        int j = 1;
        while (position.moveDest(dir, i) != null) {
            if (_board[position.moveDest(dir, i).index()] != EMP) {
                totalPiece += 1;
            }
            i += 1;
        }
        while (position.moveDest(oppositeDir, j) != null) {

            if (_board[position.moveDest(oppositeDir, j).index()] != EMP) {
                totalPiece += 1;
            }
            j += 1;
        }
        return totalPiece + 1;

    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */

    boolean isLegal(Square from, Square to) {

        int distance = to.distance(from);
        int direction = from.direction(to);
        int numPiece = numPiece(direction, from);



        boolean validMove = from.isValidMove(to);
        boolean fromIsRight = _board[from.index()] == turn();
        boolean rightStep = numPiece == distance;
        boolean block = blocked(from, to);
        boolean notCaptureOwn = _board[from.index()] != _board[to.index()];

        return validMove && fromIsRight && (!block) && rightStep
                && notCaptureOwn && distance != 0;
    }

    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        ArrayList<Move> legalMove = new ArrayList<>();
        for (Square each: ALL_SQUARES) {
            if (_board[each.index()] == turn()) {
                for (int i = 0; i < 8; i++) {
                    int distance = numPiece(i, each);
                    Square to = each.moveDest(i, distance);
                    if (to != null) {
                        Move move = Move.mv(each, to);
                        if (isLegal(move)) {
                            legalMove.add(move);

                        }

                    }
                }
            }
        }
        return legalMove;

    }


    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */

    Piece winner() {
        if (!_winnerKnown) {
            if (piecesContiguous(WP) && piecesContiguous(BP)) {
                if (!_moves.isEmpty()) {
                    Move lastMove = _moves.get(_moves.size() - 1);
                    if (_board[lastMove.getTo().index()] == WP) {
                        _winner = WP;
                    } else {
                        _winner = BP;
                    }
                    _winnerKnown = true;

                } else {
                    _winner = turn().opposite();
                    _winnerKnown = true;
                }

            } else if (piecesContiguous(WP)) {
                _winner = WP;
                _winnerKnown = true;
            } else if (piecesContiguous(BP)) {
                _winner = BP;
                _winnerKnown = true;
            } else if (_moves.size() >= _moveLimit) {
                _winner = EMP;
                _winnerKnown = true;

            } else {
                _winner = null;
            }
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {

        int i = 1;
        int dir = from.direction(to);
        int dis = from.distance(to);
        while (i != dis) {
            if (_board[from.moveDest(dir, i).index()] == turn()
                    || _board[from.moveDest(dir, i).index()] == EMP) {
                i += 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ.  VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */

    private int numContig(Square sq, boolean[][] visited, Piece p) {
        int totalContig;
        if (sq == null) {
            return 0;
        } else if (p == EMP) {
            return 0;
        } else if (_board[sq.index()] != p) {
            return 0;
        } else if (visited[sq.row()][sq.col()]) {
            return 0;
        } else {
            visited[sq.row()][sq.col()] = true;
            totalContig = 1;
            for (int i = 0; i < BOARD_SIZE; i++) {
                totalContig += numContig(sq.moveDest(i, 1), visited, p);
            }
        }
        return totalContig;
    }



    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */

    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();

        boolean [][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];



        for (Square each : ALL_SQUARES) {
            if (_board[each.index()] == WP) {
                int totalWhite = numContig(each, visited, _board[each.index()]);
                if (totalWhite > 0) {
                    _whiteRegionSizes.add(totalWhite);
                }
            } else if (_board[each.index()] == BP) {
                int totalBlack = numContig(each, visited, _board[each.index()]);
                if (totalBlack > 0) {
                    _blackRegionSizes.add(totalBlack);
                }
            }
        }


        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }
    /** @return the black region. */

    public ArrayList<Integer> whiteRegion() {
        return _whiteRegionSizes;
    }
    /** @return the white region. */
    public  ArrayList<Integer> blackRegion() {
        return _blackRegionSizes;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S. */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }


    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];

    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
        _whiteRegionSizes = new ArrayList<>(),
        _blackRegionSizes = new ArrayList<>();

}

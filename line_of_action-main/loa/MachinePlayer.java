/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;



import static loa.Piece.*;

/** An automated Player.
 *  @author Xinyu Fu
 */
class MachinePlayer extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new MachinePlayer with no piece or controller (intended to produce
     *  a template). */
    MachinePlayer() {
        this(null, null);
    }

    /** A MachinePlayer that plays the SIDE pieces in GAME. */
    MachinePlayer(Piece side, Game game) {
        super(side, game);
    }

    @Override
    String getMove() {
        Move choice;

        assert side() == getGame().getBoard().turn();
        int depth;
        choice = searchForMove();
        getGame().reportMove(choice);
        return choice.toString();
    }

    @Override
    Player create(Piece piece, Game game) {
        return new MachinePlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private Move searchForMove() {
        Board work = new Board(getBoard());
        int value;
        assert side() == work.turn();
        _foundMove = null;
        if (side() == WP) {
            value = findMove(work, chooseDepth(), true, 1, -INFTY, INFTY);
        } else {
            value = findMove(work, chooseDepth(), true, -1, -INFTY, INFTY);
        }
        return _foundMove;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove.
     *  got some help from the videos posted on piazza. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {

        Move bestMove = null;

        if (board.winner() == WP) {
            return WINNING_VALUE;
        } else if (board.winner() == BP) {
            return -WINNING_VALUE;
        }


        if (depth == 0) {
            return evaluate(board);
        }
        int bestScore = sense == 1 ? -INFTY : INFTY;

        for (Move each: board.legalMoves()) {
            board.makeMove(each);
            int score = findMove(board, depth - 1, false,
                    sense * -1, alpha, beta);
            board.retract();
            if (score * sense > bestScore * sense) {
                bestScore = score;
                bestMove = each;
            }

            if (sense == 1) {
                alpha = Math.max(alpha, score);

            } else {
                beta = Math.min(beta, score);
            }
            if (beta <= alpha) {
                break;


            }
        }
        if (saveMove) {
            _foundMove = bestMove;
        }




        return bestScore;
    }

    /** Return a search depth for the current position. */
    private int chooseDepth() {
        return 3;
    }

    /** @return evaluate the move
     * @param board . */
    private int evaluate(Board board) {


        return board.blackRegion().size() - board.whiteRegion().size();

    }


    /** Used to convey moves discovered by findMove. */
    private Move _foundMove;

}

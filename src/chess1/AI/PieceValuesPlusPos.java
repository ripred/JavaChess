package chess1.AI;

import chess1.Board;
import chess1.Move;
import chess1.Piece;
import chess1.Side;
import chess1.Spot;

import java.util.ArrayList;
import java.util.List;

/**
 * The PieceValuesPlusPos class is an implementation of the
 * BoardEvaluator interface.
 *
 * The implementation scores the board by summing the values
 * of each player's available pieces plus a bonus for the best
 * pieces controlling the center of the board.
 */
public class PieceValuesPlusPos implements BoardEvaluator {

    @Override
    public int evaluate(final Board board) {
        int value = 0;
        int mobilityBonus = 50;

        for (final Spot spot:board.getBoard()) {
            if (spot.getType() == Piece.Empty) continue;

            final int sValue = Piece.values[spot.getType()];
            if (spot.getSide() == Side.Black)
                value -= sValue;
            else
                value += sValue;

            // Add bonus for pieces being closer to the board center.
            // Higher power pieces get bigger bonus.
//            int dx = spot.getCol();
//            if (dx > 3) dx = 7 - dx;
//            int dy = spot.getRow();
//            if (dy > 3) dy = 7 - dy;
//            if (spot.getSide() == Side.Black)
//                value -= (dx + dy) * spot.getType() * 10;
//            else
//                value += (dx + dy) * spot.getType() * 10;
        }

        if (board.getAdjacentBonus()) {
            value += getAdjacentKingChecks(board);
        }

        if (board.getTurn() == Side.Black) {
            value -= board.getCurrentPlayerMoves().size() * mobilityBonus;
            value += board.getOtherPlayerMoves().size() * mobilityBonus;
        } else {
            value += board.getCurrentPlayerMoves().size() * mobilityBonus;
            value -= board.getOtherPlayerMoves().size() * mobilityBonus;
        }

        return value;
    }

    int getAdjacentKingChecks(Board board) {
        int value = 0;
        int adjBonus = 10_000;

        // Bonus for checking spots adjacent to the king
        if (board.getTurn() == Side.White) {
            List<Spot> covered = new ArrayList<>();
            loop1:      for (Move move : board.getCurrentPlayerMoves()) {
                for (Spot adjSpot : board.getBlkKingAdjacent()) {
                    for (Spot cover:covered) {
                        if (adjSpot.getCol() == cover.getCol() && adjSpot.getRow() == cover.getRow()) {
                            continue loop1;
                        }
                    }
                    if (move.getToCol() == adjSpot.getCol() && move.getToRow() == adjSpot.getRow()) {
                        value += adjBonus;
                        covered.add(adjSpot);
                    }
                }
            }
            covered = new ArrayList<>();
            loop2:      for (Move move : board.getOtherPlayerMoves()) {
                for (Spot adjSpot : board.getWhtKingAdjacent()) {
                    for (Spot cover:covered) {
                        if (adjSpot.getCol() == cover.getCol() && adjSpot.getRow() == cover.getRow()) {
                            continue loop2;
                        }
                    }
                    if (move.getToCol() == adjSpot.getCol() && move.getToRow() == adjSpot.getRow()) {
                        value -= adjBonus;
                        covered.add(adjSpot);
                    }
                }
            }
        } else {
            List<Spot> covered = new ArrayList<>();
            loop3:      for (Move move : board.getCurrentPlayerMoves()) {
                for (Spot adjSpot : board.getWhtKingAdjacent()) {
                    for (Spot cover:covered) {
                        if (adjSpot.getCol() == cover.getCol() && adjSpot.getRow() == cover.getRow()) {
                            continue loop3;
                        }
                    }
                    if (move.getToCol() == adjSpot.getCol() && move.getToRow() == adjSpot.getRow()) {
                        value -= adjBonus;
                        covered.add(adjSpot);
                    }
                }
            }
            covered = new ArrayList<>();
            loop4:      for (Move move : board.getOtherPlayerMoves()) {
                for (Spot adjSpot : board.getBlkKingAdjacent()) {
                    for (Spot cover:covered) {
                        if (adjSpot.getCol() == cover.getCol() && adjSpot.getRow() == cover.getRow()) {
                            continue loop4;
                        }
                    }
                    if (move.getToCol() == adjSpot.getCol() && move.getToRow() == adjSpot.getRow()) {
                        value += adjBonus;
                        covered.add(adjSpot);
                    }
                }
            }
        }
        return  value;
    }
}
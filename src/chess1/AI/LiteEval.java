package chess1.AI;

import chess1.LiteBoard;
import chess1.Side;

public class LiteEval {

    public int evaluate(final LiteBoard board) {
        int score = 0;
        int mobilityBonus = 3;
        int centerBonus = 10;

//        int kingMoveBonus = 100;

//        int theirKing =-1;
//        int ourKing =-1;

        for (int ndx=0; ndx < LiteBoard.BOARD_SIZE; ndx++) {
            if (board.isEmpty(ndx)) continue;
            int factor = (board.getSide(ndx) == Side.Black) ? -1 : 1;

            int type = board.getType(ndx);
            int material = board.getValue(ndx);

            score += material * factor;

//            if (type == LiteUtil.King)
//                if (spot.getSide() == board.turn)
//                    ourKing = ndx;
//                else
//                    theirKing = ndx;

            // Add bonus for pieces being closer to the board center.
            // Higher power pieces get bigger bonus.
//            int dx = ndx % 8;
//            if (dx > 3) dx = 7 - dx;
//            int dy = ndx / 8;
//            if (dy > 3) dy = 7 - dy;
//
//            score += (dx + dy) * board.getType(ndx) * centerBonus * factor;
        }

        int factor = (board.turn == Side.Black) ? -1 : 1;

//        int ourKingMoves = 0;
//        int theirKingMoves = 0;
//
//        for (int i=0; i < board.numMoves1; i++) {
//            if ((board.moves1[i].getFromCol() + board.moves1[i].getFromRow() * 8) == ourKing)
//                ourKingMoves++;
//        }
//
//        for (int i=0; i < board.numMoves2; i++) {
//            if ((board.moves2[i].getFromCol() + board.moves2[i].getFromRow() * 8) == theirKing)
//                theirKingMoves++;
//        }
//
//        score += ourKingMoves * kingMoveBonus;
//        score -= theirKingMoves * kingMoveBonus;

        score += board.numMoves1 * mobilityBonus * factor;
        score -= board.numMoves2 * mobilityBonus * factor;

        return score;
    }
}
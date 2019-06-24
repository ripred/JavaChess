package chess;

interface Piece {
    int Empty  = -1;
    int Pawn   =  1;
    int Knight =  2;
    int Bishop =  3;
    int Rook   =  4;
    int Queen  =  5;
    int King   =  6;
    int[] values = {0, 10000, 30000, 30000, 50000, 90000, Integer.MAX_VALUE - (Integer.MAX_VALUE / 4)};
}




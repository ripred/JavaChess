package chess;

interface Piece {
    int Empty  = -1;
    int Pawn   =  0;
    int Knight =  1;
    int Bishop =  2;
    int Rook   =  3;
    int Queen  =  4;
    int King   =  5;
    int[] values = {10000, 30000, 30000, 50000, 90000, Integer.MAX_VALUE};
}




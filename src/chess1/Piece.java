package chess1;

public interface Piece {
    int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE / 4);
    int MIN_VALUE = 0 - MAX_VALUE;

    int Empty  =  0;
    int Pawn   =  1;
    int Knight =  2;
    int Bishop =  3;
    int Rook   =  4;
    int Queen  =  5;
    int King   =  6;

    int[] values = {0, 10000, 31000, 30000, 50000, 90000, MAX_VALUE};
}




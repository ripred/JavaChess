package chess;

public class Spot {
    private int     side;   // Side.White or Side.Black
    private int     type;   // piece type -1...5
    private int     col;    // column location 0-7
    private int     row;    // row location 0-7
    private boolean moved;  // true if this piece has moved

    /**
     * @param side the color of this piece (valid only if type != Piece.Empty)
     * @param type  the piece type at this spot (or Piece.Empty if none)
     * @param col   the column location of this spot
     * @param row   the row    location of this spot
     */
    Spot(int side, int type, int col, int row) {
        setColor(side);
        setType(type);
        setCol(col);
        setRow(row);
        setMoved(false);
    }

    /**
     * @param spot the Spot object to be copied into this new Spot object in this copy constructor
     */
    Spot(final Spot spot) {
        side  = spot.side;
        type  = spot.type;
        col   = spot.col;
        row   = spot.row;
        moved = spot.moved;
    }

    int     getSide()   {return side;}
    int     getType()   {return type;}
    int     getCol()    {return col;}
    int     getRow()    {return row;}
    boolean getMoved()  {return moved;}
    boolean isEmpty()   {return type == Piece.Empty;}  // spot is empty if true

    void setColor(int s)        { side = s; }
    void setType(int n)         { type = n; }
    void setCol(int n)          { col = n; }
    void setRow(int n)          { row = n; }
    void setMoved(boolean b)    { moved = b; }
    void setEmpty()             { type = Piece.Empty; }

    @Override
    public String toString() {
        return String.format("[Spot side:%d type:%d col:%d row:%d mvd:%d", side, type, col, row, moved ? 1:0);
    }
}

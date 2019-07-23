import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * EN class reads and writes FEN notation to and from a LiteBoard state
 *
 */
public class FEN {

    public static void decode(LiteBoard board, String in) {
        for (int ndx=0; ndx < LiteBoard.BOARD_SIZE; ndx++) {
            board.board[ndx] = LiteUtil.newSpot(LiteBoard.Empty, 0, false, false);
        }

        int ndx = 0;
        int pos = 0;
        char c;
        while( ndx < LiteBoard.BOARD_SIZE && in.charAt(pos) != ' ') {
            c = in.charAt(pos++);
            switch (c) {
                case 'p':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Pawn, Side.Black, true, false);
                    break;
                case 'n':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Knight, Side.Black, true, false);
                    break;
                case 'b':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Bishop, Side.Black, true, false);
                    break;
                case 'r':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Rook, Side.Black, true, false);
                    break;
                case 'q':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Queen, Side.Black, true, false);
                    break;
                case 'k':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.King, Side.Black, true, false);
                    break;
                case 'P':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Pawn, Side.White, true, false);
                    break;
                case 'N':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Knight, Side.White, true, false);
                    break;
                case 'B':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Bishop, Side.White, true, false);
                    break;
                case 'R':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Rook, Side.White, true, false);
                    break;
                case 'Q':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Queen, Side.White, true, false);
                    break;
                case 'K':
                    board.board[ndx++] = LiteUtil.newSpot(LiteBoard.King, Side.White, true, false);
                    break;
                case '/':
                    assert(ndx % 8 == 0) : "Oops?\n";
                    break;
                default:
                    if (c >= '0' && c <= '9') {
                        String s = "" + c;
                        int empty = Integer.valueOf(s);
                        while (empty-- > 0) {
                            board.board[ndx++] = LiteUtil.newSpot(LiteBoard.Empty, 0, false, false);
                        }
                    }
                    break;
            }
        }

        c = in.charAt(pos++);
        if (c == ' ') c = in.charAt(pos++);

        if (c == 'w') board.turn = Side.White;
        else if (c == 'b') board.turn = Side.Black;
    }

    public static boolean read(LiteBoard board, String filename) {
        boolean result = true;

        String in = "";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            in = reader.readLine();
            reader.close();
        } catch (IOException e) {
            result = false;
        }

        decode(board, in);

        return result;
    }

    public static String encode(LiteBoard board) {
        String[] table = {" ","p","n","b","r","q","k"};
        String placement = "";
        int empty = 0;
        for (int ndx=0; ndx < LiteBoard.BOARD_SIZE; ndx++) {
            int type = board.getType(ndx);
            int side = board.getSide(ndx);

            if ((ndx > 0) && (ndx % 8) == 0) {
                if (empty > 0) {
                    placement += empty;
                    empty = 0;
                }
                placement += "/";
            }

            switch (type) {
                case LiteBoard.Empty:
                    empty++;
                    break;
                case LiteBoard.Pawn:
                case LiteBoard.Knight:
                case LiteBoard.Bishop:
                case LiteBoard.Rook:
                case LiteBoard.Queen:
                case LiteBoard.King:
                    if (empty > 0) {
                        placement += empty;
                        empty = 0;
                    }

                    placement += (side == Side.Black) ? table[type] : table[type].toUpperCase();
                    break;
            }
        }

        if (empty > 0) {
            placement += empty;
            empty = 0;
        }

        placement += (board.turn == Side.White) ? " w " : " b ";

        String castleWht = (castleQ(board, 7) ? "Q" : "") + (castleK(board, 7) ? "K" : "");
        String castleBlk = (castleQ(board, 0) ? "q" : "") + (castleK(board, 0) ? "k" : "");

        if (castleWht.isEmpty() && castleBlk.isEmpty())
            placement += "-";
        else
            placement += castleWht + castleBlk;

        placement += " - 0 0";

        return placement;
    }

    public static boolean write(LiteBoard board, String filename) {
        boolean result = true;

        String placement = encode(board);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(placement);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            result = false;
        }

        return result;
    }

    private static boolean castleQ(LiteBoard board, int row) {
        byte rQ = board.board[0 + row * 8];
        byte nQ = board.board[1 + row * 8];
        byte bQ = board.board[2 + row * 8];
        byte qQ = board.board[3 + row * 8];

        if (LiteUtil.getType(rQ) != LiteBoard.Rook) return false;
        if (LiteUtil.hasMoved(rQ)) return false;

        byte k = board.board[4 + row * 8];

        if (LiteUtil.getType(k) != LiteBoard.King) return false;
        if (LiteUtil.hasMoved(k)) return false;
        if (LiteUtil.getType(nQ) != LiteBoard.Empty) return false;
        if (LiteUtil.getType(bQ) != LiteBoard.Empty) return false;
        if (LiteUtil.getType(qQ) != LiteBoard.Empty) return false;

        return true;
    }

    private static boolean castleK(LiteBoard board, int row) {
        byte rK = board.board[7 + row * 8];
        byte nK = board.board[6 + row * 8];
        byte bK = board.board[5 + row * 8];

        if (LiteUtil.getType(rK) != LiteBoard.Rook) return false;
        if (LiteUtil.hasMoved(rK)) return false;

        byte k = board.board[4 + row * 8];

        if (LiteUtil.getType(k) != LiteBoard.King) return false;
        if (LiteUtil.hasMoved(k)) return false;
        if (LiteUtil.getType(nK) != LiteBoard.Empty) return false;
        if (LiteUtil.getType(bK) != LiteBoard.Empty) return false;

        return true;
    }

}

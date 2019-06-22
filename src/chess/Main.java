package chess;

import java.util.List;

class Main {

    private static void print() {
        System.out.println();
    }
    private static void print(String s) {
        System.out.println(s);
    }
    private static String getPieceName(final int type) {
        switch (type) {
            case Piece.Empty:
                return "Empty";
            case Piece.Pawn:
                return "Pawn";
            case Piece.Knight:
                return "Knight";
            case Piece.Bishop:
                return "Bishop";
            case Piece.Rook:
                return "Rook";
            case Piece.Queen:
                return "Queen";
            case Piece.King:
                return "King";
            default:
                return "Unknown";
        }
    }
    private static void showTurnPrompt(Board board, int pass) {
        System.out.print(String.format("%d: %s's Turn: ",
                pass, board.getTurn() == Side.White ? "White" : "Black"));
    }
    private static void showMoveDesc(final Board board, final Move move) {
        Spot from = board.getSpot(move.getFromCol(), move.getFromRow());
        Spot to = board.getSpot(move.getToCol(), move.getToRow());
        int type = from.getType();
        String pieceName = getPieceName(type);
        String cap = to.isEmpty() ? "" : "capturing " + getPieceName(to.getType());
        boolean isCastle = type == Piece.King
                && (move.getFromCol() - move.getToCol() == 2 || move.getFromCol() - move.getToCol() == -2);

        if (isCastle) {
            if (from.getCol() == 4) {
                print("Castle on queen's side");
            } else {
                print("Castle on king's side");
            }
        } else {
            print(String.format("%s from %c%d to %c%d %s",
                    pieceName,
                    move.getFromCol() + 'a',
                    8 - move.getFromRow(),
                    move.getToCol() + 'a',
                    8 - move.getToRow(),
                    cap));
        }
    }
    private static void showStats(AIMoveSelector agent, long startTime) {
        long timeSpent = (System.nanoTime() - startTime) / 1_000_000_000L;
        int numProcessed = agent.getNumMovesExamined();
        long numPerSec = (timeSpent == 0) ? 0 : numProcessed / timeSpent;
        print(String.format("%,d moves examined.  Time Spent: %,d seconds.  %,d moves per second",
                numProcessed,
                timeSpent,
                numPerSec));
    }
    private static Move getHumanMove(final Board board) {
        String prompt = "Enter your move (ex: a2 a3): ";
        String response = "";
        Move move = null;

        System.out.print(prompt);
        while (true) {
            while (true) {
                try {
                    if (System.in.available() > 0) {
                        int c = System.in.read();
                        if (c == -1 || c == '\n' || c == '\r')
                            break;
                        if (c == '\b') {    // backspace
                            if (response.length() > 1) {
                                response = response.substring(0, response.length() - 1);
                                continue;
                            }
                        }
                        response += (char) c;
                        continue;
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (response.length() < 5) {
                System.out.println("Invalid response.");
                System.out.print(prompt);
                response = "";
                continue;
            }

            response = response.toLowerCase();
            String[] parts = response.split(" ");
            if (parts.length < 2) {
                System.out.println("Invalid response.");
                System.out.print(prompt);
                response = "";
                continue;
            }

            int col1 = parts[0].charAt(0) - 'a';
            int col2 = parts[1].charAt(0) - 'a';
            int row1 = 8 - Integer.valueOf(parts[0].substring(1));
            int row2 = 8 - Integer.valueOf(parts[1].substring(1));

            if (col1 < 0 || col1 > 7 || row1 < 0 || row2 > 7) {
                System.out.println("Invalid response.");
                System.out.print(prompt);
                response = "";
                continue;
            }

            if (board.getSpot(col1, row1).isEmpty()) {
                System.out.println("There is no piece to move at " + response);
                System.out.print(prompt);
                response = "";
                continue;
            }

            int value = board.getSpot(row2, col2).getType();
            move = new Move(col1, row1, col2, row2, value);

            // Make sure it is a legal move
            Board test = new Board(board);
            int mySide = board.getTurn();
            test.executeMove(move);
            if (board.kingInCheck(test, mySide)) {
                System.out.println("That move would put your king in check!");
                System.out.print(prompt);
                response = "";
                continue;
            }
            break;
        }
        return move;
    }


    public static void main(String[] args) {
        Board board = new Board(1);

        boolean isHuman = true;

//        board.initTest7();

        print();
        board.show();

        int pass = 1;
        final int depth = 9;
        final int maxSeconds = 5 * 60;

        AIMoveSelector moveAgent = new Minimax(depth, maxSeconds);

        showTurnPrompt(board, pass);

        long startTime = System.nanoTime();
        Move move = null;

        if (isHuman && board.getTurn() == Side.White) {
            move = getHumanMove(board);
        } else {
            move = moveAgent.bestMove(board);
        }


        while (move != null) {
            showMoveDesc(board, move);

            showStats(moveAgent, startTime);

            board.executeMove(move);
            board.advanceTurn();
            print();
            board.show();

            if (board.kingInCheck(board, board.getTurn())) {
                if (board.getTurn() == 0) {
                    print("Black is in check!");
                } else {
                    print("White is in check!");
                }
            }

            pass++;

            showTurnPrompt(board, pass);

            startTime = System.nanoTime();
            if (isHuman && board.getTurn() == Side.White) {
                move = getHumanMove(board);
            } else {
                move = moveAgent.bestMove(board);
            }
        }

        boolean checkMateBlack = board.kingInCheck(board, 0);
        boolean checkMateWhite = board.kingInCheck(board, 1);
        int numMoveBlack = board.getMoves(0).size();
        int numMoveWhite = board.getMoves(1).size();

        assert numMoveBlack > 0 || numMoveWhite > 0 : "Internal error: Both players have 0 moves";
        assert checkMateBlack   && checkMateWhite   : "Internal error: Both players are in check";

        print();

        if (numMoveWhite == 0) {
            if (checkMateWhite) {
                print("Checkmate!  Black Wins!");
            } else {
                print("Stalemate!  Black Wins!");
            }
        } else if (numMoveBlack == 0) {
            if (checkMateBlack) {
                print("Checkmate!  White Wins!");
            } else {
                print("Stalemate!  White Wins!");
            }
        }

        print(String.format("%nTotal time in getMoves() = %,d microseconds", Board.timeInGetMoves));
        Board.timeInGetMoves = 0L;

        print();

        print("Goodbye");
    }
}

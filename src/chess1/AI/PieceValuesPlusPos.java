package chess1.AI;

import chess1.Board;
import chess1.Piece;
import chess1.Side;
import chess1.Spot;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.Integer.max;
import static java.lang.Math.abs;


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
    public int evaluate(final Board board, List<Integer> statList) {
        int blkMaterial = 0;
        int whtMaterial = 0;
        int blkBonus = 0;
        int whtBonus = 0;
        int blkFriendProximity = 0;
        int whtFriendProximity = 0;
        int blkFoeProximity = 0;
        int whtFoeProximity = 0;
        int blkBlocked = 0;
        int whtBlocked = 0;

        int score = 0;

        int foeScaling = 100;
        int friendScaling = 15;
        int blockedPawnScaling = 10;
        int centerBonusScaling = 5;

        Map<Integer, Map<Integer, List<Spot>>> pieces = new HashMap<>(2);
        Map<Integer, List<Spot>> blkPMap = new HashMap<>();
        blkPMap.put(Piece.Pawn, new ArrayList<>());
        blkPMap.put(Piece.Rook, new ArrayList<>());
        blkPMap.put(Piece.Knight, new ArrayList<>());
        blkPMap.put(Piece.Bishop, new ArrayList<>());
        blkPMap.put(Piece.Queen, new ArrayList<>());
        blkPMap.put(Piece.King, new ArrayList<>());
        Map<Integer, List<Spot>> whtPMap = new HashMap<>();
        whtPMap.put(Piece.Pawn, new ArrayList<>());
        whtPMap.put(Piece.Rook, new ArrayList<>());
        whtPMap.put(Piece.Knight, new ArrayList<>());
        whtPMap.put(Piece.Bishop, new ArrayList<>());
        whtPMap.put(Piece.Queen, new ArrayList<>());
        whtPMap.put(Piece.King, new ArrayList<>());
        pieces.put(Side.Black, blkPMap);
        pieces.put(Side.White, whtPMap);

        List<Spot> allPieces = new ArrayList<>();

        boolean calcKing = false;
        for (final Spot spot:board.getBoard()) {
            if (spot.isEmpty()) continue;
            int side = spot.getSide();
            int type = spot.getType();
            pieces.get(side).get(type).add(spot);

            // calc material score and bonus for pieces being in the center
            int value = Piece.values[type];

            if (side == Side.Black)
                whtMaterial += value;
            else
                blkMaterial += value;

            if (false) {
                // Don't apply center bonus to king
                if (type != Piece.King) {
                    int dx = spot.getCol();
                    if (dx > 3) dx = 7 - dx;
                    int dy = spot.getRow();
                    if (dy > 3) dy = 7 - dy;
                    int posBonus = (dx + dy) * type * centerBonusScaling;
                    if (side == Side.Black)
                        blkBonus += posBonus;
                    else
                        whtBonus += posBonus;
                }
            }
            if (calcKing)
                allPieces.add(spot);
        }
        score += whtMaterial;
        score -= blkMaterial;
        score += whtBonus;
        score -= blkBonus;


        if (calcKing) {
            // calc king protection
            {
                // get the two king positions
                if (pieces.get(Side.Black).get(Piece.King).size() > 0) {
                    Spot spot = pieces.get(Side.Black).get(Piece.King).get(0);

                    List<Spot> friends = getFriends(spot, allPieces);
                    List<Spot> guard = getGuard(spot, friends);
                    blkFriendProximity += (7 * 16 - getProximity(spot, guard)) * friendScaling;
                    List<Spot> foes = getFoes(spot, allPieces);
                    blkFoeProximity += (7 * 16 - getProximity(spot, foes)) * foeScaling;
                }

                if (pieces.get(Side.White).get(Piece.King).size() > 0) {
                    Spot spot = pieces.get(Side.White).get(Piece.King).get(0);

                    List<Spot> friends = getFriends(spot, allPieces);
                    List<Spot> guard = getGuard(spot, friends);
                    int friendProximity = getProximity(spot, guard);
                    whtFriendProximity += (7 * 16 - friendProximity) * friendScaling;
                    List<Spot> foes = getFoes(spot, allPieces);
                    int foeProximity = getProximity(spot, foes);
                    whtFoeProximity += (7 * 16 - foeProximity) * foeScaling;
                }
            }
            score += whtFriendProximity;
            score -= blkFriendProximity;
            score -= whtFoeProximity;
            score += blkFoeProximity;
        }

        if (false) {
            // calc blocked pawns by our own pieces
            {
                int side = Side.Black;
                List<Spot> pawns = pieces.get(side).get(Piece.Pawn);
                for (Spot spot : pawns) {
                    List<Spot> surround = getSurrounding(board, spot, 8);
                    List<Spot> blockers = getBlockers(spot, surround);
                    List<Spot> myBlockers = getFriends(spot, blockers);
                    blkBlocked += myBlockers.size() * blockedPawnScaling;
                }
            }
            {
                int side = Side.White;
                List<Spot> pawns = pieces.get(side).get(Piece.Pawn);
                for (Spot spot : pawns) {
                    List<Spot> surround = getSurrounding(board, spot, 8);
                    List<Spot> blockers = getBlockers(spot, surround);
                    List<Spot> myBlockers = getFriends(spot, blockers);
                    whtBlocked += myBlockers.size() * blockedPawnScaling;
                }
            }
            score -= whtBlocked;
            score += blkBlocked;
        }

        if (statList != null) {
            statList.add(0, blkMaterial);
            statList.add(1, whtMaterial);
            statList.add(2, blkBonus);
            statList.add(3, whtBonus);
            statList.add(4, blkFriendProximity);
            statList.add(5, whtFriendProximity);
            statList.add(6, blkFoeProximity);
            statList.add(7, whtFoeProximity);
            statList.add(8, blkBlocked);
            statList.add(9, whtBlocked);
        }
        return score;
    }

    //
    // For all 8 directions around the specified spot, get the first occupied
    // spot in that direction within the specified range.
    //
    private List<Spot> getSurrounding(Board board, Spot spot, int range) {
        List<Spot> list = new ArrayList<>();

        int x = spot.getCol();
        int y = spot.getRow();

        for (int dy = -1; dy < 2; ++dy) {
loop:       for (int dx = -1; dx < 2; ++dx) {
                if ((dy + dx) == 0) continue;
                for (int ty = y + dy; ty >= 0 && ty <= 7; ty += (dy==0) ? 8 : dy) {
                    for (int tx = x + dx; tx >= 0 && tx <= 7; tx += (dx==0) ? 8 : dx) {
                        int distance = distance(x, y, tx, ty);
                        if (distance <= range) {
                            Spot examine = board.getSpot(tx, ty);
                            if (examine != null && examine.getType() != Piece.Empty) {
                                list.add(examine);
                                continue loop;
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private List<Spot> getAdjacent(Board board, Spot spot) {
        List<Spot> list = new ArrayList<>();
        int range = 1;

        int x = spot.getCol();
        int y = spot.getRow();

        for (int dy = -1; dy < 2; ++dy) {
            if (y + dy < 0 || y + dy > 7) continue;
            for (int dx = -1; dx < 2; ++dx) {
                if ((dy + dx) == 0) continue;
                if (x + dx < 0 || x + dx > 7) continue;
                Spot examine = board.getSpot(x + dx, y + dy);
                list.add(examine);
            }
        }
        return list;
    }

    private int getProximity(Spot spot, List<Spot> spots) {
        int proximity = 0;
        for( Spot examine:spots) {
            proximity += distance(spot, examine);
        }
        return proximity;
    }

    private List<Spot> getBlockers(Spot spot, List<Spot> spots) {
        List<Spot> list = new ArrayList<>();
        int forward = (spot.getSide() == Side.Black) ? 1 : -1;
        for( Spot examine:spots) {
            if ((examine.getCol() == spot.getCol()) && examine.getRow() == (spot.getRow() + forward)) {
                list.add(examine);
            }
        }
        return list;
    }

    private List<Spot> getGuard(Spot spot, List<Spot> spots) {
        List<Spot> list = new ArrayList<>();
        int forward = (spot.getSide() == Side.Black) ? 1 : -1;
        for( Spot examine:spots) {
            if (Integer.signum(examine.getRow() - spot.getRow()) != Integer.signum(forward)) {
                list.add(examine);
            }
        }
        return list;
    }

    private List<Spot> getFoes(Spot spot, List<Spot> spots) {
        List<Spot> list = new ArrayList<>();
        for( Spot examine:spots) {
            if (spot.getSide() != examine.getSide()) {
                list.add(examine);
            }
        }
        return list;
    }

    private List<Spot> getFriends(Spot spot, List<Spot> spots) {
        List<Spot> list = new ArrayList<>();
        for( Spot examine:spots) {
            if (spot.getSide() == examine.getSide()) {
                list.add(examine);
            }
        }
        return list;
    }

    private List<Spot> getWithinRange(Spot spot, List<Spot> spots, int range) {
        List<Spot> list = new ArrayList<>();
        for( Spot examine:spots) {
            if (distance(spot, examine) <= range) {
                list.add(examine);
            }
        }
        return list;
    }

    private int distance(Spot start, Spot end) {
        return distance(start.getCol(), start.getRow(), end.getCol(), end.getRow());
    }

    private int distance(int x1, int y1, int x2, int y2) {
        if (x1 > x2) {
            if (y1 < y2) {
                return max((x1 - x2), (y2 - y1));
            } else {
                return max((x1 - x2), (y1 - y2));
            }
        } else {
            if (y1 < y2) {
                return max((x2 - x1), (y2 - y1));
            } else {
                return max((x2 - x1), (y1 - y2));
            }
        }
    }











}
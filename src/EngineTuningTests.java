import java.io.IOException;

public class EngineTuningTests {

    static String[] suite1 = {
            "8/8/p1p5/1p5p/1P5p/8/PPP2K1p/4R1rk w - - 0 1 bm Rf1",
            "1q1k4/2Rr4/8/2Q3K1/8/8/8/8 w - - 0 1 bm Kh6",
            "7k/5K2/5P1p/3p4/6P1/3p4/8/8 w - - 0 1 bm g5",
            "8/6B1/p5p1/Pp4kp/1P5r/5P1Q/4q1PK/8 w - - 0 32 bm Qxh4",
            "8/8/1p1r1k2/p1pPN1p1/P3KnP1/1P6/8/3R4 b - - 0 1 bm Nxd5"
    };

    static String[] suite2 = {
            "rn1qkb1r/pp2pppp/5n2/3p1b2/3P4/2N1P3/PP3PPP/R1BQKBNR w KQkq - 0 1  bm Qb3",
            "rn1qkb1r/pp2pppp/5n2/3p1b2/3P4/1QN1P3/PP3PPP/R1B1KBNR b KQkq - 1 1 bm Bc8",
            "r1bqk2r/ppp2ppp/2n5/4P3/2Bp2n1/5N1P/PP1N1PP1/R2Q1RK1 b kq - 1 10  bm Nh6; am Ne5",
            "r1bqrnk1/pp2bp1p/2p2np1/3p2B1/3P4/2NBPN2/PPQ2PPP/1R3RK1 w - - 1 12  bm b4",
            "rnbqkb1r/ppp1pppp/5n2/8/3PP3/2N5/PP3PPP/R1BQKBNR b KQkq - 3 5  bm e5",
            "rnbq1rk1/pppp1ppp/4pn2/8/1bPP4/P1N5/1PQ1PPPP/R1B1KBNR b KQ - 1 5  bm Bcx3+",
            "r4rk1/3nppbp/bq1p1np1/2pP4/8/2N2NPP/PP2PPB1/R1BQR1K1 b - - 1 12  bm Rfb8",
            "rn1qkb1r/pb1p1ppp/1p2pn2/2p5/2PP4/5NP1/PP2PPBP/RNBQK2R w KQkq c6 1 6  bm d5",
            "r1bq1rk1/1pp2pbp/p1np1np1/3Pp3/2P1P3/2N1BP2/PP4PP/R1NQKB1R b KQ - 1 9  bm Nd4",
            "rnbqr1k1/1p3pbp/p2p1np1/2pP4/4P3/2N5/PP1NBPPP/R1BQ1RK1 w - - 1 11  bm a4",
            "rnbqkb1r/pppp1ppp/5n2/4p3/4PP2/2N5/PPPP2PP/R1BQKBNR b KQkq f3 1 3  bm d5",
            "r1bqk1nr/pppnbppp/3p4/8/2BNP3/8/PPP2PPP/RNBQK2R w KQkq - 2 6  bm Bxf7+",
            "rnbq1b1r/ppp2kpp/3p1n2/8/3PP3/8/PPP2PPP/RNBQKB1R b KQ d3 1 5  am Ne4",
            "rnbqkb1r/pppp1ppp/3n4/8/2BQ4/5N2/PPP2PPP/RNB2RK1 b kq - 1 6  am Nxc4",
            "r2q1rk1/2p1bppp/p2p1n2/1p2P3/4P1b1/1nP1BN2/PP3PPP/RN1QR1K1 w - - 1 12  bm exf6",
            "r1bqkb1r/2pp1ppp/p1n5/1p2p3/3Pn3/1B3N2/PPP2PPP/RNBQ1RK1 b kq - 2 7  bm d5",
            "r2qkbnr/2p2pp1/p1pp4/4p2p/4P1b1/5N1P/PPPP1PP1/RNBQ1RK1 w kq - 1 8  am hxg4",
            "r1bqkb1r/pp3ppp/2np1n2/4p1B1/3NP3/2N5/PPP2PPP/R2QKB1R w KQkq e6 1 7  bm Bxf6+",
            "rn1qk2r/1b2bppp/p2ppn2/1p6/3NP3/1BN5/PPP2PPP/R1BQR1K1 w kq - 5 10  am Bxe6",
            "r1b1kb1r/1pqpnppp/p1n1p3/8/3NP3/2N1B3/PPP1BPPP/R2QK2R w KQkq - 3 8  am Ndb5",
            "r1bqnr2/pp1ppkbp/4N1p1/n3P3/8/2N1B3/PPP2PPP/R2QK2R b KQ - 2 11  am Kxe6",
            "r3kb1r/pp1n1ppp/1q2p3/n2p4/3P1Bb1/2PB1N2/PPQ2PPP/RN2K2R w KQkq - 3 11  bm a4",
            "r1bq1rk1/pppnnppp/4p3/3pP3/1b1P4/2NB3N/PPP2PPP/R1BQK2R w KQ - 3 7  bm Bxh7+",
            "r2qkbnr/ppp1pp1p/3p2p1/3Pn3/4P1b1/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 2 6  bm Nxe5",
            "rn2kb1r/pp2pppp/1qP2n2/8/6b1/1Q6/PP1PPPBP/RNB1K1NR b KQkq - 1 6  am Qxb3"
    };


    static String[] silentButDeadly = {
            "1qr3k1/p2nbppp/bp2p3/3p4/3P4/1P2PNP1/P2Q1PBP/1N2R1K1 b - - bm Qc7",
            "1r2r1k1/3bnppp/p2q4/2RPp3/4P3/6P1/2Q1NPBP/2R3K1 w - - bm Rc7",
            "2b1k2r/2p2ppp/1qp4n/7B/1p2P3/5Q2/PPPr2PP/R2N1R1K b k - bm O-O",
            "2b5/1p4k1/p2R2P1/4Np2/1P3Pp1/1r6/5K2/8 w - - bm Rd8",
            "2brr1k1/ppq2ppp/2pb1n2/8/3NP3/2P2P2/P1Q2BPP/1R1R1BK1 w - - bm g3",
            "2kr2nr/1pp3pp/p1pb4/4p2b/4P1P1/5N1P/PPPN1P2/R1B1R1K1 b - - bm Bf7",
            "2r1k2r/1p1qbppp/p3pn2/3pBb2/3P4/1QN1P3/PP2BPPP/2R2RK1 b k - bm O-O",
            "2r1r1k1/pbpp1npp/1p1b3q/3P4/4RN1P/1P4P1/PB1Q1PB1/2R3K1 w - - bm Rce1",
            "2r2k2/r4p2/2b1p1p1/1p1p2Pp/3R1P1P/P1P5/1PB5/2K1R3 w - - bm Kd2",
            "2r3k1/5pp1/1p2p1np/p1q5/P1P4P/1P1Q1NP1/5PK1/R7 w - - bm Rd1",
            "2r3qk/p5p1/1n3p1p/4PQ2/8/3B4/5P1P/3R2K1 w - - bm e6",
            "3b4/3k1pp1/p1pP2q1/1p2B2p/1P2P1P1/P2Q3P/4K3/8 w - - bm Qf3",
            "3n1r1k/2p1p1bp/Rn4p1/6N1/3P3P/2N1B3/2r2PP1/5RK1 w - - bm Na4 Nce4",
            "3q1rk1/3rbppp/ppbppn2/1N6/2P1P3/BP6/P1B1QPPP/R3R1K1 w - - bm Nd4",
            "3r1rk1/p1q4p/1pP1ppp1/2n1b1B1/2P5/6P1/P1Q2PBP/1R3RK1 w - - bm Bh6",
            "3r2k1/2q2p1p/5bp1/p1pP4/PpQ5/1P3NP1/5PKP/3R4 b - - bm Qd6",
            "3r2k1/p1q1npp1/3r1n1p/2p1p3/4P2B/P1P2Q1P/B4PP1/1R2R1K1 w - - bm Bc4",
            "3r4/2k5/p3N3/4p3/1p1p4/4r3/PPP3P1/1K1R4 b - - bm Kd7",
            "3r4/2R1np1p/1p1rpk2/p2b1p2/8/PP2P3/4BPPP/2R1NK2 w - - bm b4",
            "3rk2r/1b2bppp/p1qppn2/1p6/4P3/PBN2PQ1/1PP3PP/R1B1R1K1 b k - bm O-O",
            "3rk2r/1bq2pp1/2pb1n1p/p3pP2/P1B1P3/8/1P2QBPP/2RN1R1K b k - bm Be7 O-O",
            "3rkb1r/pppb1pp1/4n2p/2p5/3NN3/1P5P/PBP2PP1/3RR1K1 w - - bm Nf5",
            "3rr1k1/1pq2ppp/p1n5/3p4/6b1/2P2N2/PP1QBPPP/3R1RK1 w - - bm Rfe1",
            "4r1k1/1q1n1ppp/3pp3/rp6/p2PP3/N5P1/PPQ2P1P/3RR1K1 w - - bm Rc1",
            "4rb1k/1bqn1pp1/p3rn1p/1p2pN2/1PP1p1N1/P1P2Q1P/1BB2PP1/3RR1K1 w - - bm Qe2",
            "4rr2/2p5/1p1p1kp1/p6p/P1P4P/6P1/1P3PK1/3R1R2 w - - bm Rfe1",
            "5r2/pp1b1kpp/8/2pPp3/2P1p2P/4P1r1/PPRKB1P1/6R1 b - - bm Ke7",
            "6k1/1R5p/r2p2p1/2pN2B1/2bb4/P7/1P1K2PP/8 w - - bm Nf6+",
            "6k1/pp1q1pp1/2nBp1bp/P2pP3/3P4/8/1P2BPPP/2Q3K1 w - - bm Qc5",
            "6k1/pp2rp1p/2p2bp1/1n1n4/1PN3P1/P2rP2P/R3NPK1/2B2R2 w - - bm Rd2",
            "8/2p2kpp/p6r/4Pp2/1P2pPb1/2P3P1/P2B1K1P/4R3 w - - bm h4",
            "Q5k1/5pp1/5n1p/2b2P2/8/5N1P/5PP1/2q1B1K1 b - - bm Kh7",
            "r1b1k1nr/1p3ppp/p1np4/4p1q1/2P1P3/N1NB4/PP3PPP/2RQK2R w Kkq - bm O-O",
            "r1b1k2r/p1pp1ppp/1np1q3/4P3/1bP5/1P6/PB1NQPPP/R3KB1R b KQkq - bm O-O",
            "r1b1k2r/ppppqppp/8/2bP4/3p4/6P1/PPQPPPBP/R1B2RK1 b kq - bm O-O",
            "r1b1k2r/ppq1bppp/2n5/2N1p3/8/2P1B1P1/P3PPBP/R2Q1RK1 b kq - bm O-O",
            "r1b1kb1r/pp2qppp/2pp4/8/4nP2/2N2N2/PPPP2PP/R1BQK2R w KQkq - bm O-O",
            "r1b1qrk1/pp4b1/2pRn1pp/5p2/2n2B2/2N2NPP/PPQ1PPB1/5RK1 w - - bm Rd3",
            "r1b2rk1/1pqn1pp1/p2bpn1p/8/3P4/2NB1N2/PPQB1PPP/3R1RK1 w - - bm Rc1",
            "r1b2rk1/2qnbp1p/p1npp1p1/1p4PQ/4PP2/1NNBB3/PPP4P/R4RK1 w - - bm Qh6",
            "r1b2rk1/pp2ppbp/2n2np1/2q5/5B2/1BN1PN2/PP3PPP/2RQK2R w K - bm O-O",
            "r1b2rk1/pp4pp/1q1Nppn1/2n4B/1P3P2/2B2RP1/P6P/R2Q3K b - - bm Na6",
            "r1b2rk1/ppp1qppp/1b1n4/8/B2n4/3NN3/PPPP1PPP/R1BQK2R w KQ - bm O-O",
            "r1b2rk1/ppq1bppp/2p1pn2/8/2NP4/2N1P3/PP2BPPP/2RQK2R w K - bm O-O",
            "r1bq1rk1/1p1n1pp1/p4n1p/2bp4/8/2NBPN2/PPQB1PPP/R3K2R w KQ - bm O-O",
            "r1bq1rk1/1p2ppbp/p2p1np1/6B1/2P1P3/2N5/PP1QBPPP/R3K2R w KQ - bm O-O",
            "r1bq1rk1/1p3ppp/p1np4/3Np1b1/2B1P3/P7/1PP2PPP/RN1QK2R w KQ - bm O-O",
            "r1bq1rk1/4bppp/ppnppn2/8/2P1P3/2N5/PPN1BPPP/R1BQK2R w KQ - bm O-O",
            "r1bq1rk1/pp1n1pbp/2n1p1p1/2ppP3/8/2PP1NP1/PP1N1PBP/R1BQ1RK1 w - - bm d4",
            "r1bq1rk1/pp1pppbp/2n2np1/8/4P3/1NN5/PPP1BPPP/R1BQK2R w KQ - bm O-O",
            "r1bq1rk1/pp2ppbp/2n2np1/2p3B1/4P3/2P2N2/PP1NBPPP/R2QK2R w KQ - bm O-O",
            "r1bq1rk1/pp2ppbp/2n3p1/2p5/2BPP3/2P1B3/P3NPPP/R2QK2R w KQ - bm O-O",
            "r1bq1rk1/pp3ppp/2n1pn2/2p5/1bBP4/2N1PN2/PP3PPP/R1BQ1RK1 w - - bm a3",
            "r1bq1rk1/pp3ppp/2n2n2/3p4/8/P1NB4/1PP2PPP/R1BQK2R w KQ - bm O-O",
            "r1bq1rk1/ppp1npb1/3p2pp/3Pp2n/1PP1P3/2N5/P2NBPPP/R1BQR1K1 b - - bm Nf4",
            "r1bq1rk1/ppp2ppp/2n1pn2/3p4/1bPP4/2NBPN2/PP3PPP/R1BQK2R w KQ - bm O-O",
            "r1bq1rk1/pppp1pbp/2n2np1/4p3/2P5/P1N2NP1/1P1PPPBP/R1BQK2R w KQ - bm O-O",
            "r1bqk2r/2ppbppp/p1n2n2/1p2p3/4P3/1B3N2/PPPPQPPP/RNB2RK1 b kq - bm O-O",
            "r1bqk2r/5ppp/p1np4/1p1Np1b1/4P3/2P5/PPN2PPP/R2QKB1R b KQkq - bm O-O",
            "r1bqk2r/bp3ppp/p1n1pn2/3p4/1PP5/P1N1PN2/1B3PPP/R2QKB1R b KQkq - bm O-O",
            "r1bqk2r/p2pppbp/2p3pn/2p5/4P3/2P2N2/PP1P1PPP/RNBQR1K1 b kq - bm O-O",
            "r1bqk2r/pp2bppp/2n1p3/1B1n4/3P4/2N2N2/PP3PPP/R1BQ1RK1 b kq - bm O-O",
            "r1bqk2r/pp2bppp/2n1p3/3n4/3P4/2NB1N2/PP3PPP/R1BQ1RK1 b kq - bm O-O",
            "r1bqk2r/pp2ppbp/2np1np1/2p5/4P3/1B1P1N1P/PPP2PP1/RNBQK2R w KQkq - bm O-O",
            "r1bqk2r/ppn1bppp/2n5/2p1p3/8/2NP1NP1/PP1BPPBP/R2Q1RK1 b kq - bm O-O",
            "r1bqk2r/ppp1bppp/2n5/3p4/3P4/2PB1N2/P1P2PPP/R1BQ1RK1 b kq - bm O-O",
            "r1bqk2r/ppp2ppp/2nb4/3np3/8/PP2P3/1BQP1PPP/RN2KBNR b KQkq - bm O-O",
            "r1bqk2r/ppp2ppp/3b4/4p3/8/1PPP1N2/2PB1PPP/R2Q1RK1 b kq - bm O-O",
            "r1bqk2r/pppp1ppp/5n2/4p3/Bb2P3/5Q2/PPPPNPPP/R1B1K2R b KQkq - bm O-O",
            "r1bqkb1r/pp3ppp/2n5/2pp4/3Pn3/2N2N2/PPP1BPPP/R1BQK2R w KQkq - bm O-O",
            "r1bqkb1r/pp3ppp/2npp3/3nP3/2BP4/5N2/PP3PPP/RNBQK2R w KQkq - bm O-O",
            "r1bqkbnr/3p1ppp/p1p1p3/8/4P3/3B4/PPP2PPP/RNBQK2R w KQkq - bm O-O",
            "r1bqkbnr/ppp2ppp/2n5/8/2BpP3/5N2/PP3PPP/RNBQK2R w KQkq - bm O-O",
            "r1bqrbk1/1pp3pp/2n2p2/p2np3/8/PP1PPN2/1BQNBPPP/R3K2R w KQ - bm O-O",
            "r1br2k1/1p2qppp/pN2pn2/P7/2pn4/4N1P1/1P2PPBP/R3QRK1 b - - bm Rb8",
            "r1q1k2r/1b1nbppp/pp1ppn2/8/2PQP3/1PN2NP1/PB3PBP/R2R2K1 b kq - bm O-O",
            "r1q1k2r/pb1nbppp/1p2pn2/8/P1PNP3/2B3P1/2QN1PBP/R4RK1 b kq - bm O-O",
            "r1r3k1/1bq2pbp/pp1pp1p1/2n5/P3PP2/R2B4/1PPBQ1PP/3N1R1K w - - bm Bc3",
            "r1rn2k1/pp1qppbp/6p1/3pP3/3P4/1P3N1P/PB1Q1PP1/R3R1K1 w - - bm Rac1",
            "r2q1rk1/1b1nbpp1/pp2pn1p/8/2BN3B/2N1P3/PP2QPPP/2R2RK1 w - - bm Rfd1",
            "r2q1rk1/1b3ppp/4pn2/1pP5/1b6/2NBPN2/1PQ2PPP/R3K2R w KQ - bm O-O",
            "r2q1rk1/pb1nppbp/6p1/1p6/3PP3/3QBN1P/P3BPP1/R3K2R w KQ - bm O-O",
            "r2q1rk1/pb2bppp/npp1pn2/3pN3/2PP4/1PB3P1/P2NPPBP/R2Q1RK1 w - - bm e4",
            "r2q1rk1/pppb1pbp/2np1np1/4p3/2P5/P1NPPNP1/1P3PBP/R1BQK2R w KQ - bm O-O",
            "r2qk2r/1b1n1ppp/4pn2/p7/1pPP4/3BPN2/1B3PPP/R2QK2R w KQkq - bm O-O",
            "r2qk2r/1b2bppp/p1n1pn2/1p6/1P6/P2BPN2/1B2QPPP/RN3RK1 b kq - bm O-O",
            "r2qk2r/2p2ppp/p1n1b3/1pbpP3/4n3/1BP2N2/PP1N1PPP/R1BQ1RK1 b kq - bm O-O",
            "r2qk2r/3n1ppp/p3p3/3nP3/3R4/5N2/1P1N1PPP/3QR1K1 b kq - bm O-O",
            "r2qk2r/p1pn1ppp/b3pn2/3p4/Nb1P4/1P3NP1/P3PPBP/1RBQ1RK1 b kq - bm O-O Qe7",
            "r2qk2r/ppp1bppp/2n2n2/8/2BP2b1/2N2N2/PP3PPP/R1BQR1K1 b kq - bm O-O",
            "r2qkb1r/pb1n1p2/2p1p2p/4P1pn/PppP4/2N2NB1/1P2BPPP/R2Q1RK1 w kq - bm Ne4",
            "r2qkb1r/pp2nppp/1np1p3/4Pb2/3P4/PB3N2/1P3PPP/RNBQ1RK1 b kq - bm Ned5",
            "r2qkb1r/pp3ppp/2bppn2/8/2PQP3/2N2N2/PP3PPP/R1B1K2R w KQkq - bm O-O",
            "r2qr1k1/p3bppp/1p2n3/3Q1N2/5P2/4B1P1/PP3R1P/R5K1 w - - bm Rd1",
            "r2r2k1/p1pnqpp1/1p2p2p/3b4/3P4/3BPN2/PP3PPP/2RQR1K1 b - - bm c5",
            "r2r2k1/pp1b1ppp/8/3p2P1/3N4/P3P3/1P3P1P/3RK2R b K - bm Rac8",
            "r3k2r/1b1nb1p1/p1q1pn1p/1pp3N1/4PP2/2N5/PPB3PP/R1BQ1RK1 w kq - bm Nf3",
            "r3k2r/1pqnnppp/p5b1/1PPp1p2/3P4/2N5/P2NB1PP/2RQ1RK1 b kq - bm O-O",
            "r3k2r/p1q1nppp/1pn5/2P1p3/4P1Q1/P1P2P2/4N1PP/R1B2K1R b kq - bm O-O",
            "r3k2r/pp2pp1p/6p1/2nP4/1R2PB2/4PK2/P5PP/5bNR w kq - bm Ne2",
            "r3k2r/ppp1bppp/2n5/3n4/3PB3/8/PP3PPP/RNB1R1K1 b kq - bm O-O-O",
            "r3kb1r/pp3ppp/4bn2/3p4/P7/4N1P1/1P2PPBP/R1B1K2R w KQkq - bm O-O",
            "r3kbnr/1pp3pp/p1p2p2/8/3qP3/5Q1P/PP3PP1/RNB2RK1 w kq - bm Rd1",
            "r3kr2/pppb1p2/2n3p1/3Bp2p/4P2N/2P5/PP3PPP/2KR3R b q - bm O-O-O",
            "r3nrk1/pp2qpb1/3p1npp/2pPp3/2P1P2N/2N3Pb/PP1BBP1P/R2Q1RK1 w - - bm Re1",
            "r3r1k1/1pqn1pbp/p2p2p1/2nP2B1/P1P1P3/2NB3P/5PP1/R2QR1K1 w - - bm Rc1",
            "r3r1k1/pp1q1ppp/2p5/P2n1p2/1b1P4/1B2PP2/1PQ3PP/R1B2RK1 w - - bm e4",
            "r3r1k1/pp3ppp/2ppqn2/5R2/2P5/2PQP1P1/P2P2BP/5RK1 w - - bm Qd4",
            "r3rbk1/p2b1p2/5p1p/1q1p4/N7/6P1/PP1BPPBP/3Q1RK1 w - - bm Nc3",
            "r4r1k/pp1bq1b1/n2p2p1/2pPp1Np/2P4P/P1N1BP2/1P1Q2P1/2KR3R w - - bm Ne6",
            "r4rk1/1bqp1ppp/pp2pn2/4b3/P1P1P3/2N2BP1/1PQB1P1P/2R2RK1 w - - bm b3",
            "r4rk1/1q2bppp/p1bppn2/8/3BPP2/3B2Q1/1PP1N1PP/4RR1K w - - bm e5",
            "r4rk1/pp2qpp1/2pRb2p/4P3/2p5/2Q1PN2/PP3PPP/4K2R w K - bm O-O",
            "r7/3rq1kp/2p1bpp1/p1Pnp3/2B4P/PP4P1/1B1RQP2/2R3K1 b - - bm Rad8",
            "r7/pp1bpp2/1n1p2pk/1B3P2/4P1P1/2N5/PPP5/1K5R b - - bm Kg5",
            "rn1q1rk1/p4pbp/bp1p1np1/2pP4/8/P1N2NP1/1PQ1PPBP/R1B1K2R w KQ - bm O-O",
            "rn1q1rk1/pb3p2/1p5p/3n2P1/3p4/P4P2/1P1Q1BP1/R3KBNR b KQ - bm Re8+",
            "rn1q1rk1/pp2bppp/1n2p1b1/8/2pPP3/1BN1BP2/PP2N1PP/R2Q1RK1 w - - bm Bc2",
            "rn1q1rk1/pp3ppp/4bn2/2bp4/5B2/2NBP1N1/PP3PPP/R2QK2R w KQ - bm O-O",
            "rn1qkbnr/pp1b1ppp/8/1Bpp4/3P4/8/PPPNQPPP/R1B1K1NR b KQkq - bm Qe7",
            "rn1qr1k1/pb3p2/1p5p/3n2P1/3p4/P4P2/1P1QNBP1/R3KB1R b KQ - bm d3",
            "rn2kb1r/pp2nppp/1q2p3/3pP3/3P4/5N2/PP2NPPP/R1BQK2R w KQkq - bm O-O",
            "rn3rk1/1bqp1ppp/p3pn2/8/Nb1NP3/4B3/PP2BPPP/R2Q1RK1 w - - bm Rc1",
            "rn3rk1/pbp1qppp/1p1ppn2/8/2PP4/P1Q2NP1/1P2PPBP/R1B1K2R w KQ - bm O-O",
            "rnb1k2r/1pq2ppp/p2ppn2/2b5/3NPP2/2P2B2/PP4PP/RNBQ1R1K b kq - bm O-O",
            "rnb2rk1/ppq1ppbp/6p1/2p5/3PP3/2P2N2/P3BPPP/1RBQK2R w K - bm O-O",
            "rnbq1rk1/5ppp/p3pn2/1p6/2BP4/P1P2N2/5PPP/R1BQ1RK1 w - - bm Bd3",
            "rnbq1rk1/pp2ppbp/2pp1np1/8/P2PP3/2N2N2/1PP1BPPP/R1BQK2R w KQ - bm O-O",
            "rnbq1rk1/ppp1ppbp/6p1/8/8/2P2NP1/P2PPPBP/R1BQK2R w KQ - bm O-O",
            "rnbqk1nr/pp3pbp/2ppp1p1/8/2BPP3/2N2Q2/PPP2PPP/R1B1K1NR w KQkq - bm Nge2",
            "rnbqk2r/ppp2ppp/1b1p1n2/4p3/2B1P3/2PP1N2/PP1N1PPP/R1BQK2R b KQkq - bm O-O",
            "rnbqk2r/pppp2pp/4pn2/5p2/1b1P4/2P2NP1/PP2PPBP/RNBQK2R b KQkq - bm Be7",
            "rnbqr1k1/pp1p1ppp/5n2/3Pb3/1P6/P1N3P1/4NPBP/R1BQK2R w KQ - bm O-O",
            "rnq1nrk1/pp3pbp/6p1/3p4/3P4/5N2/PP2BPPP/R1BQK2R w KQ - bm O-O"
    };


    static String[] kaufmanTest = {
            "1rbq1rk1/p1b1nppp/1p2p3/8/1B1pN3/P2B4/1P3PPP/2RQ1R1K w - - bm Nf6+",
            "3r2k1/p2r1p1p/1p2p1p1/q4n2/3P4/PQ5P/1P1RNPP1/3R2K1 b - - bm Nxd4",
            "3r2k1/1p3ppp/2pq4/p1n5/P6P/1P6/1PB2QP1/1K2R3 w - - am Rd1",
            "r1b1r1k1/1ppn1p1p/3pnqp1/8/p1P1P3/5P2/PbNQNBPP/1R2RB1K w - - bm Rxb2",
            "2r4k/pB4bp/1p4p1/6q1/1P1n4/2N5/P4PPP/2R1Q1K1 b - - bm Qxc1",
            "r5k1/3n1ppp/1p6/3p1p2/3P1B2/r3P2P/PR3PP1/2R3K1 b - - am Rxa2",
            "2r2rk1/1bqnbpp1/1p1ppn1p/pP6/N1P1P3/P2B1N1P/1B2QPP1/R2R2K1 b - - bm Bxe4",
            "5r1k/6pp/1n2Q3/4p3/8/7P/PP4PK/R1B1q3 b - - bm h6",
            "r3k2r/pbn2ppp/8/1P1pP3/P1qP4/5B2/3Q1PPP/R3K2R w KQkq - bm Be2",
            "3r2k1/ppq2pp1/4p2p/3n3P/3N2P1/2P5/PP2QP2/K2R4 b - - bm Nxc3",
            "q3rn1k/2QR4/pp2pp2/8/P1P5/1P4N1/6n1/6K1 w - - bm Nf5",
            "6k1/p3q2p/1nr3pB/8/3Q1P2/6P1/PP5P/3R2K1 b - - bm Rd6",
            "1r4k1/7p/5np1/3p3n/8/2NB4/7P/3N1RK1 w - - bm Nxd5",
            "1r2r1k1/p4p1p/6pB/q7/8/3Q2P1/PbP2PKP/1R3R2 w - - bm Rxb2",
            "r2q1r1k/pb3p1p/2n1p2Q/5p2/8/3B2N1/PP3PPP/R3R1K1 w - - bm Bxf5",
            "8/4p3/p2p4/2pP4/2P1P3/1P4k1/1P1K4/8 w - - bm b4",
            "1r1q1rk1/p1p2pbp/2pp1np1/6B1/4P3/2NQ4/PPP2PPP/3R1RK1 w - - bm e5",
            "q4rk1/1n1Qbppp/2p5/1p2p3/1P2P3/2P4P/6P1/2B1NRK1 b - - bm Qc8",
            "r2q1r1k/1b1nN2p/pp3pp1/8/Q7/PP5P/1BP2RPN/7K w - - bm Qxd7",
            "8/5p2/pk2p3/4P2p/2b1pP1P/P3P2B/8/7K w - - bm Bg4",
            "8/2k5/4p3/1nb2p2/2K5/8/6B1/8 w - - bm Kxb5",
            "1B1b4/7K/1p6/1k6/8/8/8/8 w - - bm Ba7",
            "rn1q1rk1/1b2bppp/1pn1p3/p2pP3/3P4/P2BBN1P/1P1N1PP1/R2Q1RK1 b - - bm Ba6",
            "8/p1ppk1p1/2n2p2/8/4B3/2P1KPP1/1P5P/8 w - - bm Bxc6",
            "8/3nk3/3pp3/1B6/8/3PPP2/4K3/8 w - - bm Bxd7"
    };

    static String[] bratkoKopecTest = {
            "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - bm Qd1+",
            "3r1k2/4npp1/1ppr3p/p6P/P2PPPP1/1NR5/5K2/2R5 w - - bm d5",
            "2q1rr1k/3bbnnp/p2p1pp1/2pPp3/PpP1P1P1/1P2BNNP/2BQ1PRK/7R b - - bm f5",
            "rnbqkb1r/p3pppp/1p6/2ppP3/3N4/2P5/PPP1QPPP/R1B1KB1R w KQkq - bm e6",
            "r1b2rk1/2q1b1pp/p2ppn2/1p6/3QP3/1BN1B3/PPP3PP/R4RK1 w - - bm Nd5 a4",
            "2r3k1/pppR1pp1/4p3/4P1P1/5P2/1P4K1/P1P5/8 w - - bm g6",
            "1nk1r1r1/pp2n1pp/4p3/q2pPp1N/b1pP1P2/B1P2R2/2P1B1PP/R2Q2K1 w - - bm Nf6",
            "4b3/p3kp2/6p1/3pP2p/2pP1P2/4K1P1/P3N2P/8 w - - bm f5",
            "2kr1bnr/pbpq4/2n1pp2/3p3p/3P1P1B/2N2N1Q/PPP3PP/2KR1B1R w - - bm f5",
            "3rr1k1/pp3pp1/1qn2np1/8/3p4/PP1R1P2/2P1NQPP/R1B3K1 b - - bm Ne5",
            "2r1nrk1/p2q1ppp/bp1p4/n1pPp3/P1P1P3/2PBB1N1/4QPPP/R4RK1 w - - bm f4",
            "r3r1k1/ppqb1ppp/8/4p1NQ/8/2P5/PP3PPP/R3R1K1 b - - bm Bf5",
            "r2q1rk1/4bppp/p2p4/2pP4/3pP3/3Q4/PP1B1PPP/R3R1K1 w - - bm b4",
            "rnb2r1k/pp2p2p/2pp2p1/q2P1p2/8/1Pb2NP1/PB2PPBP/R2Q1RK1 w - - bm Qd2 Qe1",
            "2r3k1/1p2q1pp/2b1pr2/p1pp4/6Q1/1P1PP1R1/P1PN2PP/5RK1 w - - bm Qxg7+",
            "r1bqkb1r/4npp1/p1p4p/1p1pP1B1/8/1B6/PPPN1PPP/R2Q1RK1 w kq - bm Ne4",
            "r2q1rk1/1ppnbppp/p2p1nb1/3Pp3/2P1P1P1/2N2N1P/PPB1QP2/R1B2RK1 b - - bm h5",
            "r1bq1rk1/pp2ppbp/2np2p1/2n5/P3PP2/N1P2N2/1PB3PP/R1B1QRK1 b - - bm Nb3",
            "3rr3/2pq2pk/p2p1pnp/8/2QBPP2/1P6/P5PP/4RRK1 b - - bm Rxe4",
            "r4k2/pb2bp1r/1p1qp2p/3pNp2/3P1P2/2N3P1/PPP1Q2P/2KRR3 w - - bm g4",
            "3rn2k/ppb2rpp/2ppqp2/5N2/2P1P3/1P5Q/PB3PPP/3RR1K1 w - - bm Nh6",
            "2r2rk1/1bqnbpp1/1p1ppn1p/pP6/N1P1P3/P2B1N1P/1B2QPP1/R2R2K1 b - - bm Bxe4",
            "r1bqk2r/pp2bppp/2p5/3pP3/P2Q1P2/2N1B3/1PP3PP/R4RK1 b kq - bm f6",
            "r2qnrnk/p2b2b1/1p1p2pp/2pPpp2/1PP1P3/PRNBB3/3QNPPP/5RK1 w - - bm f4"
    };

    //
    //    30 points, if solution is found between 0 and 9 seconds
    //    25 points, if solution is found between 10 and 29 seconds
    //    20 points, if solution is found between 30 and 89 seconds
    //    15 points, if solution is found between 90 and 209 seconds
    //    10 points, if solution is found between 210 and 389 seconds
    //     5 points, if solution is found between 390 and 600 seconds
    //     0 points, if not found with in 10 minutes
    //    Minimum = 1900
    //    Maximum = 1900 + 30*35 = 2950
    static String[] epdSet = {
            "r3kb1r/3n1pp1/p6p/2pPp2q/Pp2N3/3B2PP/1PQ2P2/R3K2R w KQkq - bm d6",
            "1k1r3r/pp2qpp1/3b1n1p/3pNQ2/2pP1P2/2N1P3/PP4PP/1K1RR3 b - - bm Bb4",
            "r6k/pp4p1/2p1b3/3pP3/7q/P2B3r/1PP2Q1P/2K1R1R1 w - - bm Qc5",
            "1nr5/2rbkppp/p3p3/Np6/2PRPP2/8/PKP1B1PP/3R4 b - - bm e5",
            "2r2rk1/1p1bq3/p3p2p/3pPpp1/1P1Q4/P7/2P2PPP/2R1RBK1 b - - bm Bb5",
            "3r1bk1/p4ppp/Qp2p3/8/1P1B4/Pq2P1P1/2r2P1P/R3R1K1 b - - bm e5",
            "r1b2r1k/pp2q1pp/2p2p2/2p1n2N/4P3/1PNP2QP/1PP2RP1/5RK1 w - - bm Nd1",
            "r2qrnk1/pp3ppb/3b1n1p/1Pp1p3/2P1P2N/P5P1/1B1NQPBP/R4RK1 w - - bm Bh3",
            "5nk1/Q4bpp/5p2/8/P1n1PN2/q4P2/6PP/1R4K1 w - - bm Qd4",
            "r3k2r/3bbp1p/p1nppp2/5P2/1p1NP3/5NP1/PPPK3P/3R1B1R b kq - bm Bf8",
            "bn6/1q4n1/1p1p1kp1/2pPp1pp/1PP1P1P1/3N1P1P/4B1K1/2Q2N2 w - - bm h4",
            "3r2k1/pp2npp1/2rqp2p/8/3PQ3/1BR3P1/PP3P1P/3R2K1 b - - bm Rb6",
            "1r2r1k1/4ppbp/B5p1/3P4/pp1qPB2/2n2Q1P/P4PP1/4RRK1 b - - bm Nxa2",
            "r2qkb1r/1b3ppp/p3pn2/1p6/1n1P4/1BN2N2/PP2QPPP/R1BR2K1 w kq - bm d5",
            "1r4k1/1q2bp2/3p2p1/2pP4/p1N4R/2P2QP1/1P3PK1/8 w - - bm Nxd6",
            "rn3rk1/pbppq1pp/1p2pb2/4N2Q/3PN3/3B4/PPP2PPP/R3K2R w KQ - bm Qxh7+",
            "4r1k1/3b1p2/5qp1/1BPpn2p/7n/r3P1N1/2Q1RPPP/1R3NK1 b - - bm Qf3",
            "2k2b1r/1pq3p1/2p1pp2/p1n1PnNp/2P2B2/2N4P/PP2QPP1/3R2K1 w - - bm exf6",
            "2r2r2/3qbpkp/p3n1p1/2ppP3/6Q1/1P1B3R/PBP3PP/5R1K w - - bm Rxh7+",
            "2r1k2r/2pn1pp1/1p3n1p/p3PP2/4q2B/P1P5/2Q1N1PP/R4RK1 w q - bm exf6",
            "2rr2k1/1b3ppp/pb2p3/1p2P3/1P2BPnq/P1N3P1/1B2Q2P/R4R1K b - - bm Rxc3",
            "2b1r1k1/r4ppp/p7/2pNP3/4Q3/q6P/2P2PP1/3RR1K1 w - - bm Nf6+",
            "6k1/5p2/3P2p1/7n/3QPP2/7q/r2N3P/6RK b - - bm Rxd2",
            "rq2rbk1/6p1/p2p2Pp/1p1Rn3/4PB2/6Q1/PPP1B3/2K3R1 w - - bm Bxh6",
            "rnbq2k1/p1r2p1p/1p1p1Pp1/1BpPn1N1/P7/2P5/6PP/R1B1QRK1 w - - bm Nxh7",
            "r2qrb1k/1p1b2p1/p2ppn1p/8/3NP3/1BN5/PPP3QP/1K3RR1 w - - bm e5",
            "8/1p3pp1/7p/5P1P/2k3P1/8/2K2P2/8 w - - bm f6",
            "8/pp2r1k1/2p1p3/3pP2p/1P1P1P1P/P5KR/8/8 w - - bm f5",
            "8/3p4/p1bk3p/Pp6/1Kp1PpPp/2P2P1P/2P5/5B2 b - - bm Bxe4",
            "5k2/7R/4P2p/5K2/p1r2P1p/8/8/8 b - - bm h3; am h5",
            "6k1/6p1/7p/P1N5/1r3p2/7P/1b3PP1/3bR1K1 w - - bm a6",
            "8/3b4/5k2/2pPnp2/1pP4N/pP1B2P1/P3K3/8 b - - bm f4",
            "6k1/4pp1p/3p2p1/P1pPb3/R7/1r2P1PP/3B1P2/6K1 w - - bm Bb4",
            "2k5/p7/Pp1p1b2/1P1P1p2/2P2P1p/3K3P/5B2/8 w - - bm c5",
            "8/5Bp1/4P3/6pP/1b1k1P2/5K2/8/8 w - - bm Kg4"
    };

    static public void runTestSuites() {
        LiteBoard board = new LiteBoard();

//        String[][] allSuites = {
//            suite1,
//            suite2,
//            silentButDeadly,
//            kaufmanTest,
//            bratkoKopecTest,
//            epdSet
//        };
//        runCatalog(board, allSuites);

        runSuite(board, kaufmanTest);
    }

    public static void runCatalog(LiteBoard board, String[][] catalog) {
        for (String[] suite : catalog) {
            runSuite(board, suite);
        }
    }

    public static void runSuite(LiteBoard board, String[] suite) {
        for (String test : suite) {
            runTest(board, test);
        }
    }

    public static void runTest(LiteBoard board, String fen) {
        FEN.decode(board, fen);
//        LiteBoard.dumpBoard(board);

        board.turn = (board.turn + 1) % 2;
        board.advanceTurn();
        board.turns = board.turn;

        int numPawns = 0;
        for (byte p : board.board) {
            if (LiteUtil.getType(p) == LiteBoard.Pawn)
                numPawns++;
        }
//        System.out.printf("%d pawns. %d seconds to solve.\n\n", numPawns, numPawns * 60);

        String comp = FEN.encode(board);
//        System.out.println(fen);
//        System.out.println(comp);

        LiteMinimax agent = new LiteMinimax("", 6, numPawns * 60);

        long moveStart = System.nanoTime();

        Move move = agent.bestMove(board, true);
        while (move == null) {
            Main.showBoard(board, fen, agent, moveStart, 0, true);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (agent.moveSearchIsDone()) {
                move = agent.getBestMove();
                if (move == null) {
                    System.out.println("no move found");
                }
            }
            try {
                if (System.in.available() > 0) {
                    if (System.in.read() == '\n') {
                        // see if we have a best move so far:
                        move = agent.getBestMove();
                        if (move != null) {
                            break;
                        }
                        // go on to the next test
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int pos = fen.length() - 1;
        for (; pos > fen.length() - 6; pos--) {
            if (fen.charAt(pos) == ' ')
                break;
        }

        System.out.println(
                Main.getPieceName(board.getType(move.getFromCol() + move.getFromRow() * 8)) + " "
                        + Main.posString(move.getFromCol(), move.getFromRow())
                        + " to "
                        + Main.posString(move.getToCol(), move.getToRow())
                        + " ? " + fen.substring(pos) + "    "
        );
        System.out.println();
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

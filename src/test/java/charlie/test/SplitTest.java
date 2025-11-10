package charlie.test;

import charlie.actor.Arriver;
import charlie.actor.ClientAuthenticator;
import charlie.actor.Courier;
import charlie.card.Card;
import charlie.card.Hand;
import charlie.card.Hid;
import charlie.dealer.Seat;
import charlie.message.view.to.SplitResponse;
import charlie.plugin.IUi;
import charlie.server.Ticket;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

/**
 * This class is a demo of a simple but plausible unit test case of Split logic.
 * @author Tyler DeLorey
 */
public class SplitTest extends AbstractTestCase implements IUi {
    final int BET_AMT = 5;
    final int SIDE_BET_AMT = 0;
    Hid you;

    // Split hands
    Hid split1;
    Hid split2;

    final Boolean gameOver = false;
    Courier courier = null;
    Boolean myTurn = false;

    // Added hands for split case
    Hand myHand = null;
    Hand mySplitHand1 = null;
    Hand mySplitHand2 = null;

    // track total winnings
    private double totalWinnings = 0.0;

    /**
     * Runs the test.
     */
    public void test() throws Exception {
        // Start the server
        go();

        // Authentication looks for these properties
        Properties props = System.getProperties();
        props.load(new FileInputStream("Split.props"));

        // Connect to game server securely.
        ClientAuthenticator authenticator = new ClientAuthenticator();
        Ticket ticket = authenticator.send("tester", "123");
        info("connecting to server");

        // Start the courier which sends messages to & receives messages from the server
        courier = new Courier(this);
        courier.start();
        info("courier started");

        // Tell the game server we've arrived.
        new Arriver(ticket).send();
        info("we ARRIVED!");

        // Wait for dealer to call READY
        synchronized (this) {
            info("waiting for server READY...");
            this.wait();
        }

        info("server READY !");

        courier.bet(BET_AMT, SIDE_BET_AMT);
        info("bet amt: " + BET_AMT + ", side bet: " + SIDE_BET_AMT);

        // Wait for dealer to call end of game
        synchronized (gameOver) {
            info("waiting ENDING...");
            gameOver.wait();
        }

        info("DONE !");
    }

    /**
     * Invoked whenever a card is dealt.
     */
    @Override
    public void deal(Hid hid, Card card, int[] handValues) {
        info("DEAL: " + hid + " card: " + card + " hand values: " + handValues[0] + ", " + handValues[1]);

        // Assign cards to correct hands
        if (hid.getSeat() == Seat.YOU) {
            if (hid.equals(you)) {
                assert myHand != null : "bad hand";
                myHand.hit(card);
            } else if (hid.equals(split1)) {
                assert mySplitHand1 != null : "bad split 1 hand";
                mySplitHand1.hit(card);
            } else if (hid.equals(split2)) {
                assert mySplitHand2 != null : "bad split 2 hand";
                mySplitHand2.hit(card);
            }
        }

        // Validate the cards received
        if (hid.equals(split1) && mySplitHand1.size() == 2) {
            assert card.getRank() == 4 && card.getSuit() == Card.Suit.CLUBS :
                    "Expected C4 for first split hand, got " + card;
        } else if (hid.equals(split2) && mySplitHand2.size() == 2) {
            assert card.getRank() == 3 && card.getSuit() == Card.Suit.CLUBS :
                    "Expected C3 for second split hand, got " + card;
        }
    }

    /**
     * Invoked when turn changes.
     */
    @Override
    public void turn(Hid hid) {
        if (hid.getSeat() != Seat.YOU) {
            myTurn = false;
            return;
        }

        myTurn = true;

        // If this is the original hand (before split)
        if (hid.equals(you)) {
            assert myHand.size() == 2;
            assert myHand.getCard(0).getRank() == 9;
            assert myHand.getCard(1).getRank() == 9;

            info("Splitting pair of 9s...");
            new Thread(() -> courier.split(you)).start();
        }
        // If this is a split hand
        else if (hid.equals(split1)) {
            info("Staying on first split hand...");
            new Thread(() -> courier.stay(split1)).start();
        }
        else if (hid.equals(split2)) {
            info("Staying on second split hand...");
            new Thread(() -> courier.stay(split2)).start();
        }
    }

    /**
     * Invoked if a hand breaks.
     */
    @Override
    public void bust(Hid hid) {
        info("BREAK: " + hid);
        assert false;
    }

    /**
     * Invoked for a winning hand.
     */
    @Override
    public void win(Hid hid) {
        info("WIN: " + hid);
        Seat who = hid.getSeat();
        assert who == Seat.YOU : "you didn't win " + who + " did";
        double pl = hid.getAmt();

        // Accepts normal win
        assert pl == BET_AMT : "unexpected P&L: " + pl;

        // update total winnings
        totalWinnings += pl;
    }

    /**
     * Invoked for a losing hand.
     */
    @Override
    public void lose(Hid hid) {
        info("LOSE: " + hid);
        Seat who = hid.getSeat();
        assert who == Seat.DEALER : "dealer didn't win " + who + " did";
        double pl = hid.getAmt();

        // Accept normal or double-down loss
        assert pl == BET_AMT : "unexpected P&L: " + pl;

        // subtract loss from total winnings
        totalWinnings -= pl;
    }

    /**
     * Invoked for a push.
     */
    @Override
    public void push(Hid hid) {
        info("PUSH: " + hid + " (net change $0)");
        assert false;
    }

    /**
     * Invoked for a natural Blackjack.
     */
    @Override
    public void blackjack(Hid hid) {
        info("BLACKJACK: " + hid);
        assert false;
    }

    /**
     * Invoked for a 5-card Charlie hand.
     */
    @Override
    public void charlie(Hid hid) {
        assert false;
    }

    /**
     * Invoked at start of a game before any cards are dealt.
     */
    @Override
    public void starting(List<Hid> hids, int shoeSize) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("game STARTING: ");

        for (Hid hid : hids) {
            buffer.append(hid).append(", ");
            if (hid.getSeat() == Seat.YOU) {
                this.you = hid;
                myHand = new Hand(you);
            }
        }
        buffer.append(" shoe size: ").append(shoeSize);
        info(buffer.toString());
    }

    /**
     * Invoked after a game ends and before a new one starts.
     */
    @Override
    public void ending(int shoeSize) {
        synchronized (gameOver) {
            gameOver.notify();
        }

        info("ENDING game shoe size: " + shoeSize);
        info("TOTAL WINNINGS: $" + totalWinnings);
    }

    /**
     * Invoked when the burn card appears.
     */
    @Override
    public void shuffling() {
        info("SHUFFLING");
        assert false;
    }

    @Override
    public void setCourier(Courier courier) { }

    /**
     * Invoked when a player requests a split.
     */
    @Override
    public void split(Hid newHid, Hid origHid) {
        info("SPLIT: new Hand " + newHid + " from " + origHid);

        split1 = origHid;
        split2 = newHid;

        mySplitHand1 = new Hand(split1);
        mySplitHand2 = new Hand(split2);

        info("Assigned Split 1 " + split1);
        info("Assigned Split 2 " + split2);
    }
}

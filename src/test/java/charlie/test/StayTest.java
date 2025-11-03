/*
 * Copyright (c) Ron Coleman
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package charlie.test;

import charlie.actor.Arriver;
import charlie.actor.ClientAuthenticator;
import charlie.actor.Courier;
import charlie.card.Card;
import charlie.card.Hid;
import charlie.dealer.Seat;
import charlie.plugin.IUi;
import charlie.server.Ticket;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

/**
 * This class is a demo of a simple but plausible unit test case of STAY logic.
 * @author Ron.Coleman
 */
public class StayTest extends AbstractTestCase implements IUi {
    // Class-level bet constants for consistency
    final int BET_AMT = 5;
    final int SIDE_BET_AMT = 0;

    Hid you;
    final Boolean gameOver = false;
    Courier courier = null;

    // Track total net winnings (YOU perspective)
    private double totalWinnings = 0.0;

    /**
     * Runs the test.
     */
    public void test() throws Exception {
        // Start the server
        go();

        // Load properties
        Properties props = System.getProperties();
        props.load(new FileInputStream("charlie.props"));

        // Connect to game server securely.
        ClientAuthenticator authenticator = new ClientAuthenticator();
        Ticket ticket = authenticator.send("tester","123");
        info("connecting to server");

        // Start the courier which sends/receives after arrival.
        courier = new Courier(this);
        courier.start();
        info("courier started");

        // Tell the game server we've arrived.
        new Arriver(ticket).send();
        info("we ARRIVED!");

        // Wait for READY
        synchronized (this) {
            info("waiting for server READY...");
            this.wait();
        }
        info("server READY !");

        // Start a game by betting
        courier.bet(BET_AMT, SIDE_BET_AMT);
        info("bet amt: " + BET_AMT + ", side bet: " + SIDE_BET_AMT);

        // Wait for dealer to call end of game.
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
    }

    /**
     * Invoked only once whenever the turn changes.
     */
    @Override
    public void turn(Hid hid) {
        if (hid.getSeat() == Seat.YOU)
            new Thread(() -> courier.stay(you)).start();
    }

    /**
     * Invoked if a hand breaks.
     */
    @Override
    public void bust(Hid hid) {
        info("BREAK: " + hid);
    }

    /**
     * Invoked for a winning hand.
     */
    @Override
    public void win(Hid hid) {
        info("WIN: " + hid);
        double pl = hid.getAmt();

        if (hid.getSeat() == Seat.YOU) {
            totalWinnings += pl;            // your win increases net
        } else if (hid.getSeat() == Seat.DEALER) {
            totalWinnings -= Math.abs(pl);  // dealer win decreases your net
        }
    }

    /**
     * Invoked for a losing hand.
     */
    @Override
    public void lose(Hid hid) {
        info("LOSE: " + hid);
        double pl = hid.getAmt();

        if (hid.getSeat() == Seat.YOU) {
            totalWinnings += pl;            // likely negative; adds as a subtraction
        } else if (hid.getSeat() == Seat.DEALER) {
            totalWinnings += Math.abs(pl);  // dealer loss increases your net
        }
    }

    /**
     * Invoked for a push (tie).
     */
    @Override
    public void push(Hid hid) {
        info("PUSH: " + hid + " (net change $0)");
    }

    /**
     * Invoked for a natural Blackjack.
     */
    @Override
    public void blackjack(Hid hid) {
        info("BLACKJACK: " + hid);
        double pl = hid.getAmt();
        if (hid.getSeat() == Seat.YOU) {
            totalWinnings += pl;
        } else if (hid.getSeat() == Seat.DEALER) {
            totalWinnings -= Math.abs(pl);
        }
    }

    /**
     * Invoked for a 5-card Charlie hand.
     */
    @Override
    public void charlie(Hid hid) {
        // Not possible for this test case.
        assert false;
    }

    /**
     * Invoked at the start of a game before any cards are dealt.
     */
    @Override
    public void starting(List<Hid> hids, int shoeSize) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("game STARTING: ");

        for (Hid hid : hids) {
            buffer.append(hid).append(", ");
            if (hid.getSeat() == Seat.YOU)
                this.you = hid;
        }
        buffer.append(" shoe size: ").append(shoeSize);
        info(buffer.toString());
    }

    /**
     * Invoked after a game ends and before the start of a new game.
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
     * Invoked when the burn card appears (re-shuffle coming).
     */
    @Override
    public void shuffling() {
        info("SHUFFLING");
    }

    /**
     * Not used here because the test case instantiates a courier.
     */
    @Override
    public void setCourier(Courier courier) { }

    /**
     * Invoked when a player requests a split.
     */
    @Override
    public void split(Hid newHid, Hid origHid) {
        // Not possible for this test case.
        assert false;
    }
}

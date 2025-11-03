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
import charlie.card.Hand;
import charlie.card.Hid;
import charlie.dealer.Seat;
import charlie.plugin.IUi;
import charlie.server.Ticket;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

/**
 * This class is a demo of a simple but plausible unit test case of DoubleDown logic.
 * @author Elizabeth Herrera
 */
public class DoubleDTest extends AbstractTestCase implements IUi {
    final int BET_AMT = 5;
    final int SIDE_BET_AMT = 0;
    Hid you;
    final Boolean gameOver = false;
    Courier courier = null;
    Boolean myTurn = false;
    Hand myHand = null;

    // --- Added: track total winnings ---
    private double totalWinnings = 0.0;

    /**
     * Runs the test.
     */
    public void test() throws Exception {
        // Start the server
        go();

        // Authentication looks for these properties
        Properties props = System.getProperties();
        props.load(new FileInputStream("DoubleD.props"));

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

        if (hid.getSeat() == Seat.YOU) {
            assert myHand != null : "bad hand";
            myHand.hit(card);

            if (myTurn) {
                assert myHand.size() == 3;
                assert card.getRank() == 4 && card.getSuit() == Card.Suit.CLUBS;
                new Thread(() -> courier.stay(you)).start();
            }
        }
    }

    /**
     * Invoked when turn changes.
     */
    @Override
    public void turn(Hid hid) {
        if (hid.getSeat() == Seat.YOU) {
            myTurn = true;
            new Thread(() -> courier.dubble(you)).start();
        } else
            myTurn = false;
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
        Seat who = hid.getSeat();
        assert who == Seat.YOU : "you didn't win " + who + " did";
        double pl = hid.getAmt();

        // Accept normal win or double-down win
        assert pl == (double) BET_AMT || pl == (double) (BET_AMT * 2)
                : "unexpected P&L: " + pl;

        // --- Added: update total winnings ---
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
        assert pl == (double) BET_AMT || pl == (double) (BET_AMT * 2)
                : "unexpected P&L: " + pl;

        // --- Added: subtract loss from total winnings ---
        totalWinnings -= pl;
    }

    /**
     * Invoked for a push.
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
    }

    @Override
    public void setCourier(Courier courier) { }

    /**
     * Invoked when a player requests a split.
     */
    @Override
    public void split(Hid newHid, Hid origHid) {
        assert false;
    }
}

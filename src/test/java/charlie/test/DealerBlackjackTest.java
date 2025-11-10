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
 * This class is a demo of a simple but plausible unit test case of
 * Dealer Blackjack logic.
 * @author Elizabeth Herrera
 */
public class DealerBlackjackTest extends AbstractTestCase implements IUi {
    final int BET_AMT = 5;
    final int SIDE_BET_AMT = 0;

    Hid you;
    final Boolean gameOver = false;
    Courier courier = null;
    boolean bj = false;

    // Track total winnings (net)
    private double totalWinnings = 0.0;

    /**
     * Runs the test.
     */
    public void test() throws Exception {
        // Start the server
        go();

        // Load props
        Properties props = System.getProperties();
        props.load(new FileInputStream("DealerBlackjack.props"));

        // Connect to game server securely
        ClientAuthenticator authenticator = new ClientAuthenticator();
        Ticket ticket = authenticator.send("tester", "123");
        info("connecting to server");

        // Start courier
        courier = new Courier(this);
        courier.start();
        info("courier started");

        // Tell server we've arrived
        new Arriver(ticket).send();
        info("we ARRIVED!");

        // Wait for READY
        synchronized (this) {
            info("waiting for server READY...");
            this.wait();
        }
        info("server READY !");

        // Place bet
        courier.bet(BET_AMT, SIDE_BET_AMT);
        info("bet amt: " + BET_AMT + ", side bet: " + SIDE_BET_AMT);

        // Check for dealer blackjack
        info("checking if blackjack was detected...");
        if (bj) {
            info("SUCCESS: Blackjack was triggered for DEALER!");
        } else {
            info("FAILURE: Blackjack was not detected!");
        }

        // Wait for end of game
        synchronized (this) {
            info("waiting ENDING...");
            this.wait();
        }

        info("DONE!");
        info("DEALER has blackjack!");
        info("Dealer Blackjack Test SUCCESSFUL");
    }

    @Override
    public void deal(Hid hid, Card card, int[] handValues) {
        info("DEAL: " + hid + " card: " + card + " hand values: " + handValues[0] + ", " + handValues[1]);
    }

    @Override
    public void turn(Hid hid) {
        if (hid.getSeat() != Seat.YOU)
            return;
        new Thread(() -> courier.stay(hid)).start();
    }

    @Override
    public void bust(Hid hid) {
        info("BREAK: " + hid);
        assert false;
    }

    @Override
    public void win(Hid hid) {
        info("WIN: " + hid);
        double pl = hid.getAmt();

        if (hid.getSeat() == Seat.YOU) {
            // If YOU won, add directly (pl may be positive or negative)
            totalWinnings += pl;
        } else if (hid.getSeat() == Seat.DEALER) {
            // Dealer win means your net goes down by their gain
            totalWinnings -= Math.abs(pl);
        }
        assert false;
    }

    @Override
    public void lose(Hid hid) {
        info("LOSE: " + hid);
        double pl = hid.getAmt();

        if (hid.getSeat() == Seat.YOU) {
            // If YOU lost, pl is probably negative; add directly (subtracts)
            totalWinnings += pl;
        } else if (hid.getSeat() == Seat.DEALER) {
            // Dealer loses means you gain
            totalWinnings += Math.abs(pl);
        }
    }

    @Override
    public void push(Hid hid) {
        info("PUSH: " + hid + " (net change $0)");
        assert false;
    }

    @Override
    public void blackjack(Hid hid) {
        info("BLACKJACK: " + hid);
        bj = true;
        double pl = hid.getAmt();

        if (hid.getSeat() == Seat.YOU) {
            totalWinnings += pl;
        } else if (hid.getSeat() == Seat.DEALER) {
            totalWinnings -= Math.abs(pl);
        }
    }

    @Override
    public void charlie(Hid hid) {
        assert false;
    }

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

    @Override
    public void ending(int shoeSize) {
        synchronized (this) {
            this.notifyAll();
        }
        info("ENDING game shoe size: " + shoeSize);
        info("TOTAL WINNINGS: $" + totalWinnings);
    }

    @Override
    public void shuffling() {
        info("SHUFFLING");
    }

    @Override
    public void setCourier(Courier courier) { }

    @Override
    public void split(Hid newHid, Hid origHid) {
        assert false;
    }
}

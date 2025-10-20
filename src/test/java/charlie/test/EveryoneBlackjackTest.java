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
 * This class is a  demo of a simple but plausible unit test case of
 * User and Dealer Blackjack logic.
 * @author Elizabeth Herrera
 */
public class EveryoneBlackjackTest extends AbstractTestCase implements IUi {
    Hid you;
    final Boolean gameOver = false;
    Courier courier = null;
    boolean bj = false;

    /**
     * Runs the test.
     */
    public void test() throws Exception {
        // Start the server
        go();

        // Authentication looks for these properties
        Properties props = System.getProperties();
        props.load(new FileInputStream("charlie.props"));

        // Connect to game server securely.
        ClientAuthenticator authenticator = new ClientAuthenticator();

        Ticket ticket = authenticator.send("tester","123");
        info("connecting to server");

        // Start the courier which sends messages to & receive messages from the serve
        // except only after we've arrived.
        courier = new Courier(this);

        courier.start();
        info("courier started");

        // Tell the game server we've arrived.
        // Note: this is only used for arriving after which courier becomes the
        // intermediary.
        new Arriver(ticket).send();
        info("we ARRIVED!");

        // Game server will be ready when it notifies us; see Courier.got(:Ready).
        synchronized (this) {
            info("waiting for server READY...");
            this.wait();
        }

        info("server READY !");

        // Now that the game server is ready, to start a game, we just need to
        // send in a bet which in the GUI is like pressing DEAL.
        final int BET_AMT = 5;
        final int SIDE_BET_AMT = 0;

        courier.bet(BET_AMT,SIDE_BET_AMT);

        info("bet amt: "+BET_AMT+", side bet: "+SIDE_BET_AMT);

        // Wait for YOU turn -- this works for heads up game, maybe not otherwise.
        //synchronized (this) {
        //    info("waiting YOU turn...");
        //    this.wait();
        //}

        // End game cleanly.
        synchronized (this) {
            info("waiting ENDING...");
            this.wait();
        }
        info("DONE!");
        info("YOU and the DEALER have blackjack!");
        info("Test SUCCESSFUL");
    }
    /**
     * This method gets invoked whenever a card is dealt.
     * @param hid Target hand
     * @param card Card
     * @param handValues Hand value and soft value
     */
    @Override
    public void deal(Hid hid, Card card, int[] handValues) {
        info("DEAL: "+hid+" card: "+card+" hand values: "+handValues[0]+", "+handValues[1]);
    }

    /**
     * This method gets invoked only once whenever the turn changes.
     * @param hid New hand's turn
     */
    @Override
    public void turn(Hid hid) {
        // If it is not your turn, return
        if (hid.getSeat() != Seat.YOU)
            return;

        // Sends stay message to server side
        new Thread(() -> {
            courier.stay(hid);
        }).start();
    }

    /**
     * This method gets invoked if a hand breaks.
     * @param hid Target hand
     */
    @Override
    public void bust(Hid hid) {
        info("BREAK: "+hid);
    }

    /**
     * This method gets invoked for a winning hand.
     * @param hid Target hand
     */
    @Override
    public void win(Hid hid) {
        info("WIN: "+hid);
    }

    /**
     * This method gets invoked for a losing hand.
     * @param hid Target hand
     */
    @Override
    public void lose(Hid hid) {
        info("LOSE: "+hid);
    }

    /**
     * This method gets invoke for a hand that pushes, ie, has same value as dealer's hand.
     * @param hid Target hand
     */
    @Override
    public void push(Hid hid) {
        info("PUSH: "+hid);
    }

    /**
     * This method gets invoked for a (natural) Blackjack hand, Ace+K, Ace+Q, etc.
     * @param hid Target hand
     */
    @Override
    public void blackjack(Hid hid) {
        info("BLACKJACK: "+hid);
        bj = true;
    }

    /**
     * This method gets invoked for a 5-card Charlie hand.
     * @param hid Target hand
     */
    @Override
    public void charlie(Hid hid) {
        // Not possible for this test case.
        assert false;
    }

    /**
     * This method get invoked at the start of a game before any cards are dealt.
     * @param hids Hands in the game
     * @param shoeSize Current shoe size, ie, original shoe less cards dealt
     */
    @Override
    public void starting(List<Hid> hids, int shoeSize) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("game STARTING: ");

        for(Hid hid: hids) {
            buffer.append(hid).append(", ");
            if(hid.getSeat() == Seat.YOU)
                this.you = hid;
        }
        buffer.append(" shoe size: ").append(shoeSize);
        info(buffer.toString());
    }

    /**
     * This method gets invoked after a game ends and before the start of a new game.
     * @param shoeSize Endind shoe size
     */
    @Override
    public void ending(int shoeSize) {
        synchronized(this) {
            this.notifyAll();
        }

        info("ENDING game shoe size: "+shoeSize);
    }

    /**
     * This method gets invoked when the burn card appears, it indicates a
     * re-shuffle is coming after the current game ends.
     */
    @Override
    public void shuffling() {
        info("SHUFFLING");
    }

    /**
     * This method sets the courier.
     * It's not used here because the test case instantiates a courier.
     * @param courier Courier
     */
    @Override
    public void setCourier(Courier courier) {

    }

    /**
     * This method gets invoked when a player requests a split.
     * For instance, a 4+4 split results in two hands, each with two cards,
     * 4+x and 4+y where "x" and "y" are hits to each hand which the dealer
     * automatically performs, respectively.
     * @param newHid New hand split from the original.
     * @param origHid Original hand.
     */
    @Override
    public void split(Hid newHid, Hid origHid) {
        // Not possible for this test case.
        assert false;
    }
}

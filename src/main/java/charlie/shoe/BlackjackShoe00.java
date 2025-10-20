/*
 * Copyright (c) 2025 Hexant, LLC
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package charlie.shoe;

import charlie.card.Card;

/**
 * This class creates a shoe to test the blackjack behavior.
 * @author Ron.Coleman
 */
public class BlackjackShoe00 extends Shoe {
    @Override
    public void init() {
        cards.clear();

        // GAME 1
        // YOU
        cards.add(new Card(Card.ACE, Card.Suit.SPADES));

        // DEALER
        cards.add(new Card(Card.QUEEN, Card.Suit.HEARTS));

        // YOU
        cards.add(new Card(Card.KING, Card.Suit.HEARTS));

        // DEALER
        cards.add(new Card(6, Card.Suit.CLUBS));

        // GAME 2
        // YOU
        cards.add(new Card(2, Card.Suit.SPADES));

        // DEALER
        cards.add(new Card(Card.QUEEN, Card.Suit.HEARTS));

        // YOU
        cards.add(new Card(3, Card.Suit.HEARTS));

        // DEALER
        cards.add(new Card(6, Card.Suit.CLUBS));

        // YOU
        cards.add(new Card(10, Card.Suit.HEARTS));

        // DEALER
        cards.add(new Card(5, Card.Suit.HEARTS));
    }
}

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

package charlie.test;

import charlie.server.GameServer;
import org.junit.Test;

/**
 * This class tests starting the server to interact with a robot test.
 * We extend thread and use the @Test annotation, otherwise JUnit won't be happy.
 * @author Ron.Coleman
 */
public class GameServerGoTest extends Thread {
    /**
     * Runs the test.
     */
    @Test
    public void test() throws InterruptedException {
        // Launch the server in background
        new Thread(this).start();

        // Wait to give a chance to manually run a test.
        Thread.sleep(30000);

        // Expect server to stop when this (main?) thread ends.
    }

    /**
     * Enters here when thread starts to run game server in the background.
     */
    @Override
    public void run() {
        new GameServer().go();
    }
}

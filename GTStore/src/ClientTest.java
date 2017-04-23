/**
 * GTStore
 */
public class ClientTest {

    public static void main(String args[]) {
        assertTest("Put", testPut());
    }

    private static boolean testPut() {
        Client c = new Client("127.0.0.1");
        return c.put("key", 40) >= 0;
    }

    private static void assertTest(String s, boolean bool) {
        if (bool) {
            System.out.printf("%s: Passed\n", s);
        } else {
            System.out.printf("%s: FAILED\n", s);
        }
    }
}

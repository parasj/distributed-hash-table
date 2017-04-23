/**
 * GTStore
 */
public class ClientTest {

    public static void main(String args[]) {
        Client c = new Client("127.0.0.1");
        Integer k1 = (Integer) c.get("key");
        int p1 = c.put("key", 1);
        int p2 = c.put("key1", 2);
        int p3 = c.put("key2", 3);
        Integer k2 = (Integer) c.get("key");
        Integer k3 = (Integer) c.get("key1");
        Integer k4 = (Integer) c.get("key2");
        int p4 = c.put("key", 1);
        Integer k5 = (Integer) c.get("key");
    }

    private static void assertTest(String s, boolean bool) {
        if (bool) {
            System.out.printf("%s: Passed\n", s);
        } else {
            System.out.printf("%s: FAILED\n", s);
        }
    }
}

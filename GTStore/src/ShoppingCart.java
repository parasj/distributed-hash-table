import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GTStore
 */
public class ShoppingCart {
    private Client<Integer, HashSet<String>> client;
    private int id;

    private static String[] inventory = {"Zucchini", "Potatoes", "Nectarines", "Squash", "Lime", "Potato", "Lettuce",
            "Bananas", "Celery", "Cauliflower", "Cabbage", "Peppers", "Tomatoes", "Cherries", "Peas", "Berries",
            "Onions", "Watermelon", "Mushrooms", "Melons", "Asparagus", "Corn", "Pineapples", "Peaches", "Apples",
            "Lemons", "Strawberries", "Broccoli", "Carrots", "Grapes", "Oranges", "Garlic", "Pears"};

    public ShoppingCart(int id) {
        this.id = id;
        this.client = new Client<>("127.0.0.1");
        client.put(id, new HashSet<>());
    }

    public void test(int nIterations) {
        for (int i = 0; i < nIterations; i++) {
            if (!verifyCartEmpty()) {
                System.err.printf("ERROR - cart not empty before ID %d\n", id);
            }

            long putStart = System.nanoTime();
            addToShoppingCart();
            long putEnd = System.nanoTime();

            if (!verifyCartFull()) {
                System.err.printf("ERROR - cart not full on ID %d\n", id);
            }

            long removeStart = System.nanoTime();
            removeFromShoppingCart();
            long removeEnd = System.nanoTime();

            if (!verifyCartEmpty()) {
                System.err.printf("ERROR - cart not empty on ID %d\n", id);
            }

            System.out.printf("{\"id\": %d, \"iteration\": %d, \"putTime\": %d, \"removeTime\": %d, \"qps\": %.2f}\n",
                    id, i, putEnd - putStart, removeEnd - removeStart,
                    inventory.length * 1000000000.0 / (putEnd - putStart + removeEnd - removeStart));
        }
    }

    private void addToShoppingCart() {
        for (String s : inventory) {
            HashSet<String> hs = client.get(id);
            if (hs == null) {
                System.err.printf("HashSet fetch failed! got a null value for client %d with adding item `%s`\n", id, s);
            } else {
                hs.add(s);
                if (!client.put(id, hs))
                    System.err.printf("HashSet update failed for client %d with adding item `%s` and %d items in cart\n", id, s, hs.size());
            }
        }
    }

    private void removeFromShoppingCart() {
        for (String s : inventory) {
            HashSet<String> hs = client.get(id);
            if (hs == null) {
                System.err.printf("HashSet fetch failed! got a null value for client %d with removing item `%s`\n", id, s);
            } else {
                hs.remove(s);
                if (!client.put(id, hs))
                    System.err.printf("HashSet update failed for client %d with removing item `%s` and %d items in cart\n", id, s, hs.size());
            }
        }
    }

    private boolean verifyCartFull() {
        HashSet<String> hs = client.get(id);
        if (hs == null) {
            System.err.printf("HashSet fetch failed! got a null value for client %d when verifying full cart\n", id);
            return false;
        }
        return hs.size() == inventory.length;
    }

    private boolean verifyCartEmpty() {
        HashSet<String> hs = client.get(id);
        if (hs == null) {
            System.err.printf("HashSet fetch failed! got a null value for client %d when verifying empty cart\n", id);
            return false;
        }
        return hs.size() == 0;
    }

    public static void main(String[] args) throws InterruptedException {
        int nThreads = 20;
        int nIterations = 20;

        IntStream.range(0, nThreads).parallel()
                .mapToObj(ShoppingCart::new)
                .forEach(sc -> sc.test(nIterations));
    }

}

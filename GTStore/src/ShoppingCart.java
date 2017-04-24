import java.util.HashSet;

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

    public boolean test(int iterations) {
        boolean fail = false;
        for (int i = 0; i < iterations; i++) {

            if (!verifyCartEmpty()) {
                System.err.printf("ERROR - cart not empty before iteration %d", i);
                fail = true;
            }

            long putStart = System.nanoTime();
            addToShoppingCart();
            long putEnd = System.nanoTime();

            if (!verifyCartFull()) {
                System.err.printf("ERROR - cart not full on iteration %d", i);
                fail = true;
            }

            long removeStart = System.nanoTime();
            removeFromShoppingCart();
            long removeEnd = System.nanoTime();

            if (!verifyCartEmpty()) {
                System.err.printf("ERROR - cart not empty on iteration %d", i);
                fail = true;
            }

            System.out.printf("{\"id\": %d, \"iteration\": %d, \"putTime\": %d, \"removeTime\": %d, \"qps\": %.2f}\n",
                    id, i, putEnd - putStart, removeEnd - removeStart,
                    inventory.length * 1000000000.0 / (putEnd - putStart + removeEnd - removeStart));
        }

        return fail;
    }

    private void addToShoppingCart() {
        for (String s : inventory) {
            HashSet<String> hs = client.get(id);
            hs.add(s);
            if (!client.put(id, hs))
                System.err.printf("HashSet update failed for client %d with adding item `%s` and %d items in cart\n", id, s, hs.size());
        }
    }

    private void removeFromShoppingCart() {
        for (String s : inventory) {
            HashSet<String> hs = client.get(id);
            hs.remove(s);
            if (!client.put(id, hs))
                System.err.printf("HashSet update failed for client %d with removing item `%s` and %d items in cart\n", id, s, hs.size());
        }
    }

    private boolean verifyCartFull() {
        HashSet<String> hs = client.get(id);
        return hs.size() == inventory.length;
    }

    private boolean verifyCartEmpty() {
        HashSet<String> hs = client.get(id);
        return hs.size() == 0;
    }

    public static void main(String[] args) {
        ShoppingCart cart = new ShoppingCart(1);
        cart.test(20);
    }

}

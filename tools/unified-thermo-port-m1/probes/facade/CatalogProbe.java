public final class CatalogProbe {
    public static void main(String[] args) {
        try {
            com.wormzjl.createcheme.science.material.MaterialCatalog.bundled();
            System.out.println("bundled ok");
        } catch (Throwable t) {
            for (Throwable c = t; c != null; c = c.getCause()) System.out.println(c);
        }
    }
}

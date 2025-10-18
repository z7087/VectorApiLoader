import me.z7087.ial.IncubatorApiLoader;

public final class Vector {
    public static void main(String[] args) {
        IncubatorApiLoader loader = IncubatorApiLoader.of("jdk.incubator.vector", "jdk.incubator.vector.Vector");
        loader.tryLoad();
        try {
            System.out.println(Class.forName("jdk.incubator.vector.Vector"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

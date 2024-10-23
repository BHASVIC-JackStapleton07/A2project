public class Main {
    public static void main(String[] args) {
        Simulator sim = new Simulator();

        // gui setup
        GUI gui = new GUI(sim);
        gui.setup();
    }
}

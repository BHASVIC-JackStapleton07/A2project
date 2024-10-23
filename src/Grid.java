public class Grid {
    private Cell[][] cells;

    public Grid(int width, int height) {
        cells = new Cell[width][height];
        initializeCells();
    }

    // intiailise cells
    private void initializeCells() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    // accessor
    public Cell getCell(int x, int y) {
        return cells[x][y];
    }

    // update cell specific
    public void setCell(int x, int y, Cell cell) {
        cells[x][y] = cell;
    }

    //grid dimension getters
    public int getWidth() {
        return cells.length;
    }

    public int getHeight() {
        return cells[0].length;
    }
}


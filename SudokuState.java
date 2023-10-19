// Sudoku puzzle state.
package sudoku;

import java.io.*;

import java.util.*;


class SudokuState {
    // Number grid.
    char[] grid;

    // Evaluated value.
    double value;

    // Constructors.
    SudokuState(char[] grid) {
        this.grid = new char[81];

        for (int i = 0; i < 81; i++) {
            this.grid[i] = grid[i];
        }

        value = 0.0;
    }

    SudokuState() {
        this.grid = new char[81];

        for (int i = 0; i < 81; i++) {
            grid[i] = 0;
        }

        value = 0.0;
    }

    // Get number in grid.
    // Return 0 for empty cell.
    int getNum(int x, int y) {
        return grid[x + (y * 9)];
    }

    // Set number in grid.
    void setNum(int x, int y, int num) {
        grid[x + (y * 9)] = (char) num;
    }

    // How many numbers are in the grid?
    int gridCount() {
        int x;
        int y;
        int c;

        c = 0;

        for (y = 0; y < 9; y++) {
            for (x = 0; x < 9; x++) {
                if (getNum(x, y) > 0) {
                    c++;
                }
            }
        }

        return c;
    }

    // How many numbers in given row?
    int rowCount(int y) {
        int x;
        int c;

        for (x = c = 0; x < 9; x++) {
            if (getNum(x, y) > 0) {
                c++;
            }
        }

        return c;
    }

    // How many numbers in given column?
    int colCount(int x) {
        int y;
        int c;

        for (y = c = 0; y < 9; y++) {
            if (getNum(x, y) > 0) {
                c++;
            }
        }

        return c;
    }

    // How many numbers in the box containing x and y?
    int boxCount(int x, int y) {
        int x2;
        int y2;
        int c;

        x = ((x / 3) * 3) + 1;
        y = ((y / 3) * 3) + 1;
        c = 0;

        for (y2 = y - 1; y2 <= (y + 1); y2++) {
            for (x2 = x - 1; x2 <= (x + 1); x2++) {
                if (getNum(x2, y2) > 0) {
                    c++;
                }
            }
        }

        return c;
    }

    // Count occurrences of a number in a row.
    int numInRow(int y, int num) {
        int x;
        int c;

        for (x = c = 0; x < 9; x++) {
            if (getNum(x, y) == num) {
                c++;
            }
        }

        return c;
    }

    // Count occurrences of a number in a column.
    int numInCol(int x, int num) {
        int y;
        int c;

        for (y = c = 0; y < 9; y++) {
            if (getNum(x, y) == num) {
                c++;
            }
        }

        return c;
    }

    // Count occurrences of a number in a box.
    int numInBox(int x, int y, int num) {
        int x2;
        int y2;
        int c;

        x = ((x / 3) * 3) + 1;
        y = ((y / 3) * 3) + 1;
        c = 0;

        for (y2 = y - 1; y2 <= (y + 1); y2++) {
            for (x2 = x - 1; x2 <= (x + 1); x2++) {
                if (getNum(x2, y2) == num) {
                    c++;
                }
            }
        }

        return c;
    }

    // Can given number be placed at x, y?
    boolean placeOK(int x, int y, int num) {
        if (getNum(x, y) > 0) {
            return false;
        }

        if (numInRow(y, num) > 0) {
            return false;
        }

        if (numInCol(x, num) > 0) {
            return false;
        }

        if (numInBox(x, y, num) > 0) {
            return false;
        }

        return true;
    }

    // Is this a valid state?
    boolean isValid() {
        int x;
        int y;
        int i;

        for (y = 0; y < 9; y++) {
            for (x = 0; x < 9; x++) {
                i = getNum(x, y);

                if ((i < 0) || (i > 9)) {
                    return false;
                }

                for (i = 1; i <= 9; i++) {
                    if (numInRow(y, i) > 1) {
                        return false;
                    }

                    if (numInCol(x, i) > 1) {
                        return false;
                    }

                    if (numInBox(x, y, i) > 1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Is given state a duplicate of this?
    boolean isDuplicate(SudokuState state) {
        for (int i = 0; i < 81; i++) {
            if (state.grid[i] != grid[i]) {
                return false;
            }
        }

        return true;
    }

    // Get state value.
    double getValue() {
        return value;
    }

    // Set state value.
    void setValue(double value) {
        this.value = value;
    }

    // Clone.
    SudokuState cloneState() {
        SudokuState state = new SudokuState();

        for (int i = 0; i < 81; i++)
            state.grid[i] = grid[i];

        return state;
    }

    // Load from file.
    void load(String filename) {
        int x;
        int y;
        int n;
        String s;
        char[] buf = new char[9];
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(filename));

            for (y = 0; y < 9; y++) {
                s = in.readLine();

                if (s == null) {
                    throw new IOException("Unexpected EOF");
                }

                s.getChars(0, 9, buf, 0);

                for (x = 0; x < 9; x++) {
                    if ((buf[x] < '1') || (buf[x] > '9')) {
                        n = 0;
                    } else {
                        n = buf[x] - '0';
                    }

                    setNum(x, y, n);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading file " + filename + ":" +
                e.toString());
            System.exit(1);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }
    }

    // Save to file.
    void save(String filename) {
        int x;
        int y;
        int n;
        PrintWriter out = null;

        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

            for (y = 0; y < 9; y++) {
                for (x = 0; x < 9; x++) {
                    n = getNum(x, y);

                    if (n == 0) {
                        out.print(" ");
                    } else {
                        out.print("" + n);
                    }
                }

                out.println();
            }
        } catch (IOException e) {
            System.err.println("Error saving file " + filename + ":" +
                e.toString());
            System.exit(1);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    // Print.
    void print() {
        int x;
        int y;
        int n;

        for (x = 0; x < 19; x++)
            System.out.print("-");

        System.out.println();

        for (y = 0; y < 9; y++) {
            System.out.print("|");

            for (x = 0; x < 9; x++) {
                n = getNum(x, y);

                if (n > 0) {
                    System.out.print(n + "|");
                } else {
                    System.out.print(" |");
                }
            }

            System.out.println();

            for (x = 0; x < 19; x++)
                System.out.print("-");

            System.out.println();
        }
    }
}
;

/*

Solve the Sudoku puzzle using various search strategies.

Sudoku is a number puzzle on 9x9 grid consisting of 9 3x3
subgrids. Given a grid partially filled in with numbers from 1-9,
the goal is to fill in the remaining cells such that every column
and row contains 1-9, and every subgrid contains 1-9 as well.
Prints out the solution if found.

Command-line options:
-loadfile <initial input file> [-savefile <solution output file>]
-strategy <depth | breadth | best>
-repeatcheck <true | false>

File format (example):

690304015
000901000
174582936
006807300
050409080
007605200
439756128
000103000
560208093

Note: 0=empty cell

*/
package sudoku;

import java.io.*;

import java.util.*;


public class Sudoku {
    // Search strategy.
    static final int DEPTH = 0;
    static final int BREADTH = 1;
    static final int BEST = 2;

    // Command-line options.
    static final String Usage = "sudoku -loadfile <initial input file> [-savefile <solution output file>]\n\t-strategy <depth | breadth | best> -repeatcheck <true | false>";
    int Strategy;

    // Prevent repeated states?
    boolean RepeatCheck;

    // Open list.
    Vector OpenList;

    // Closed list.
    Vector ClosedList;

    // Count of expanded states.
    int ExpandCount;

    // Grid of possible values.
    boolean[][][] Maybe;

    // Using command-line options?
    boolean commandlineUse;

    // Load and save files.
    String loadfile;

    // Load and save files.
    String savefile;

    // Constructor
    public Sudoku(String[] args) {
        int i;
        String buf;
        boolean gotStrategy;
        boolean gotRepeatCheck;

        OpenList = new Vector();
        ClosedList = new Vector();
        Maybe = new boolean[9][9][10];

        loadfile = savefile = null;
        gotStrategy = gotRepeatCheck = false;
        commandlineUse = true;

        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-loadfile")) {
                i++;

                if (i >= args.length) {
                    System.err.println(Usage);
                    System.exit(1);
                }

                loadfile = args[i];

                continue;
            }

            if (args[i].equals("-savefile")) {
                i++;

                if (i >= args.length) {
                    System.err.println(Usage);
                    System.exit(1);
                }

                savefile = args[i];

                continue;
            }

            if (args[i].equals("-strategy")) {
                i++;

                if (i >= args.length) {
                    System.err.println(Usage);
                    System.exit(1);
                }

                if (args[i].equals("depth")) {
                    Strategy = DEPTH;
                } else if (args[i].equals("breadth")) {
                    Strategy = BREADTH;
                } else if (args[i].equals("best")) {
                    Strategy = BEST;
                } else {
                    System.err.println("Invalid search strategy option");
                    System.err.println(Usage);
                    System.exit(1);
                }

                gotStrategy = true;

                continue;
            }

            if (args[i].equals("-repeatcheck")) {
                i++;

                if (i >= args.length) {
                    System.err.println(Usage);
                    System.exit(1);
                }

                if (args[i].equals("true")) {
                    RepeatCheck = true;
                } else if (args[i].equals("false")) {
                    RepeatCheck = false;
                } else {
                    System.err.println("Invalid repeatcheck option");
                    System.err.println(Usage);
                    System.exit(1);
                }

                gotRepeatCheck = true;

                continue;
            }

            System.err.println(Usage);
            System.exit(1);
        }

        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(
                        System.in));

            // Get missing input.
            if (loadfile == null) {
                commandlineUse = false;
                System.out.print("Enter the puzzle load file name: ");
                loadfile = stdin.readLine();
            }

            if (!gotStrategy) {
                commandlineUse = false;
                System.out.print(
                    "Enter search strategy (depth, breadth, best): ");
                buf = stdin.readLine();

                if (buf.equals("depth")) {
                    Strategy = DEPTH;
                } else if (buf.equals("breadth")) {
                    Strategy = BREADTH;
                } else if (buf.equals("best")) {
                    Strategy = BEST;
                } else {
                    System.err.println("Invalid search strategy");
                    System.exit(1);
                }

                gotStrategy = true;
            }

            if (!gotRepeatCheck) {
                commandlineUse = false;
                System.out.print("Prevent repeating states (y|n)?: ");
                buf = stdin.readLine();

                if (buf.startsWith("Y") || buf.startsWith("y")) {
                    RepeatCheck = true;
                } else {
                    RepeatCheck = false;
                }

                gotRepeatCheck = true;
            }
        } catch (IOException e) {
            System.err.println("Cannot read stdin:" + e.toString());
            System.exit(1);
        }
    }

    // Main.
    public static void main(String[] args) {
        Sudoku sudoku = new Sudoku(args);
        sudoku.solve();
    }

    // Solve the puzzle.
    public void solve() {
        String buf;

        // Load initial state.
        SudokuState state = new SudokuState();
        state.load(loadfile);

        // Check the initial state.
        if (!state.isValid()) {
            System.err.println("Invalid initial state");
            System.exit(1);
        }

        // Search for solution.
        System.out.println("Initial puzzle:");
        state.print();
        OpenList.addElement(state);

        if ((state = search()) != null) {
            System.out.println("Found solution!");
            state.print();
            System.out.println(ExpandCount + " states expanded");

            if (!commandlineUse && (savefile == null)) {
                System.out.print("Save solution to file (y|n)?: ");

                try {
                    BufferedReader stdin = new BufferedReader(new InputStreamReader(
                                System.in));
                    buf = stdin.readLine();

                    if (buf.startsWith("Y") || buf.startsWith("y")) {
                        System.out.print("Enter file name: ");
                        savefile = stdin.readLine();
                    }
                } catch (IOException e) {
                    System.err.println("Cannot read stdin:" + e.toString());
                    System.exit(1);
                }
            }

            if (savefile != null) {
                state.save(savefile);
            }
        } else {
            System.out.println("No solution!");
            System.out.println(ExpandCount + " states expanded");
        }
    }

    // Search
    SudokuState search() {
        int x;
        int y;
        int i;
        int c;
        SudokuState state;
        SudokuState child;

        // Get initial state.
        ExpandCount = 0;

        if (OpenList.size() == 0) {
            return null;
        }

        state = (SudokuState) OpenList.elementAt(0);
        OpenList.remove(0);

        // Deduce numbers.
        deduce(state);

        // Check for solution.
        if (state.gridCount() == 81) {
            return state;
        }

        // While there are states to explore.
        while (true) {
            // Increment expansion count.
            ExpandCount++;

            // Put on closed list.
            ClosedList.addElement(state);

            // Expand the state.
            for (y = 0; y < 9; y++) {
                for (x = 0; x < 9; x++) {
                    // Count number of possible choices for this cell.
                    for (i = 1, c = 0; i <= 9; i++) {
                        if (state.placeOK(x, y, i)) {
                            c++;
                        }
                    }

                    for (i = 1; i <= 9; i++) {
                        if (state.placeOK(x, y, i)) {
                            child = state.cloneState();
                            child.setNum(x, y, i);

                            // Deduce numbers.
                            deduce(child);

                            // Check for solution.
                            if (child.gridCount() == 81) {
                                return child;
                            }

                            // Children with fewer choices are more valuable.
                            child.setValue(((double) child.gridCount() * 10.0) +
                                (double) (9 - c));

                            // Check for repeat and put on open list.
                            if (!repeat(child)) {
                                merge(child);
                            }
                        }
                    }
                }
            }

            // Get next state to expand.
            if (OpenList.size() == 0) {
                return null;
            }

            state = (SudokuState) OpenList.elementAt(0);
            OpenList.remove(0);
        }
    }

    // Deduce missing numbers in grid.
    void deduce(SudokuState state) {
        int x;
        int y;
        int z;
        int i;
        int c;

        // Deduce and fill in all possible numbers.
        boolean done = false;

        while (!done) {
            done = true;

            // Fill in the initial Maybe values.
            for (x = 0; x < 9; x++) {
                for (y = 0; y < 9; y++) {
                    for (z = 0; z < 10; z++) {
                        Maybe[x][y][z] = false;
                    }

                    for (z = 1; z < 10; z++) {
                        if (z == state.getNum(x, y)) {
                            Maybe[x][y][z] = true;
                        } else {
                            if (state.placeOK(x, y, z)) {
                                Maybe[x][y][z] = true;
                            }
                        }
                    }
                }
            }

            // Eliminate incompatible Maybe values.
            for (x = 0; x < 9; x++) {
                for (y = 0; y < 9; y++) {
                    if (state.getNum(x, y) > 0) {
                        continue;
                    }

                    for (z = 1; z < 10; z++) {
                        if (Maybe[x][y][z]) {
                            Maybe[x][y][z] = boxCheck(x, y, z);
                        }
                    }
                }
            }

            // Lone Maybe values become new grid values.
            for (x = 0; x < 9 && done; x++) {
                for (y = 0; y < 9 && done; y++) {
                    if (state.getNum(x, y) > 0) {
                        continue;
                    }

                    for (z = 1, c = i = 0; z < 10; z++) {
                        if (Maybe[x][y][z]) {
                            c++;
                            i = z;
                        }
                    }

                    if (c == 1) {
                        state.setNum(x, y, i);
                        done = false;
                    }
                }
            }
        }
    }

    // Check given number against other boxes.
    boolean boxCheck(int x, int y, int num) {
        int cx;
        int cy;
        int cx2;
        int cy2;

        // Find center of this box.
        cx = ((x / 3) * 3) + 1;
        cy = ((y / 3) * 3) + 1;

        // Check columns in boxes above and below.
        // If given number must be in the same column
        // then there is no possibility of the number
        // being at the given position.
        cx2 = cx;
        cy2 = cy - 6;

        if ((cy2 >= 0) && !columnCheck(x, cx2, cy2, num)) {
            return false;
        }

        cy2 = cy - 3;

        if ((cy2 >= 0) && !columnCheck(x, cx2, cy2, num)) {
            return false;
        }

        cy2 = cy + 6;

        if ((cy2 < 9) && !columnCheck(x, cx2, cy2, num)) {
            return false;
        }

        cy2 = cy + 3;

        if ((cy2 < 9) && !columnCheck(x, cx2, cy2, num)) {
            return false;
        }

        // Check rows in boxes left and right.
        // If given number must be in the same row
        // then there is no possibility of the number
        // being at the given position.
        cx2 = cx - 6;
        cy2 = cy;

        if ((cx2 >= 0) && !rowCheck(y, cx2, cy2, num)) {
            return false;
        }

        cx2 = cx - 3;

        if ((cx2 >= 0) && !rowCheck(y, cx2, cy2, num)) {
            return false;
        }

        cx2 = cx + 6;

        if ((cx2 < 9) && !rowCheck(y, cx2, cy2, num)) {
            return false;
        }

        cx2 = cx + 3;

        if ((cx2 < 9) && !rowCheck(y, cx2, cy2, num)) {
            return false;
        }

        return true;
    }

    // Check if number can be in box other than in given column.
    boolean columnCheck(int col, int cx, int cy, int num) {
        int x;
        int y;

        for (y = cy - 1; y <= (cy + 1); y++) {
            for (x = cx - 1; x <= (cx + 1); x++) {
                if ((x != col) && Maybe[x][y][num]) {
                    return true;
                }
            }
        }

        return false;
    }

    // Check if number can be in box other than in given row.
    boolean rowCheck(int row, int cx, int cy, int num) {
        int x;
        int y;

        for (y = cy - 1; y <= (cy + 1); y++) {
            for (x = cx - 1; x <= (cx + 1); x++) {
                if ((y != row) && Maybe[x][y][num]) {
                    return true;
                }
            }
        }

        return false;
    }

    // Merge state into open list.
    void merge(SudokuState state) {
        switch (Strategy) {
        case DEPTH:
            OpenList.insertElementAt(state, 0);

            return;

        case BREADTH:
            OpenList.addElement(state);

            return;

        case BEST:

            for (int i = 0; i < OpenList.size(); i++) {
                if (state.getValue() >= ((SudokuState) OpenList.elementAt(i)).getValue()) {
                    OpenList.insertElementAt(state, i);

                    return;
                }
            }

            OpenList.addElement(state);

            return;
        }
    }

    // State is repeating?
    boolean repeat(SudokuState state) {
        if (!RepeatCheck) {
            return false;
        }

        for (int i = 0; i < ClosedList.size(); i++) {
            if (state.isDuplicate((SudokuState) ClosedList.elementAt(i))) {
                return true;
            }
        }

        return false;
    }
}
;

// Sudoku puzzle state.

#ifndef __SUDOKU_STATE__
#define __SUDOKU_STATE__

#include <stdio.h>
#include <stdlib.h>

// Sudoku puzzle state.
class SudokuState
{
    public:
        // Constructors.
        SudokuState(char *grid)
        {
            for (int i = 0; i < 81; i++)
            {
                this->grid[i] = grid[i];
            }
            value = 0.0;
        }
        SudokuState()
        {
            for (int i = 0; i < 81; i++)
            {
                grid[i] = 0;
            }
            value = 0.0;
        }

        // Get number in grid.
        // Return 0 for empty cell.
        inline int getNum(int x, int y)
        {
            return grid[x + (y * 9)];
        }

        // Set number in grid.
        inline void setNum(int x, int y, int num)
        {
            grid[x + (y * 9)] = num;
        }

        // How many numbers are in the grid?
        int gridCount()
        {
            int x,y,c;

            c = 0;
            for (y = 0; y < 9; y++)
            {
                for (x = 0; x < 9; x++)
                {
                    if (getNum(x,y) > 0) c++;
                }
            }
            return c;
        }

        // How many numbers in given row?
        int rowCount(int y)
        {
            int x,c;

            for (x = c = 0; x < 9; x++)
            {
                if (getNum(x,y) > 0) c++;
            }
            return c;
        }

        // How many numbers in given column?
        int colCount(int x)
        {
            int y,c;

            for (y = c = 0; y < 9; y++)
            {
                if (getNum(x,y) > 0) c++;
            }
            return c;
        }

        // How many numbers in the box containing x and y?
        int boxCount(int x, int y)
        {
            int x2,y2,c;

            x = ((x / 3) * 3) + 1;
            y = ((y / 3) * 3) + 1;
            c = 0;
            for (y2 = y - 1; y2 <= y + 1; y2++)
            {
                for (x2 = x - 1; x2 <= x + 1; x2++)
                {
                    if (getNum(x2,y2) > 0) c++;
                }
            }
            return c;
        }

        // Count occurrences of a number in a row.
        int numInRow(int y, int num)
        {
            int x,c;
            for (x = c = 0; x < 9; x++)
            {
                if (getNum(x,y) == num) c++;
            }
            return c;
        }

        // Count occurrences of a number in a column.
        int numInCol(int x, int num)
        {
            int y,c;

            for (y = c = 0; y < 9; y++)
            {
                if (getNum(x,y) == num) c++;
            }
            return c;
        }

        // Count occurrences of a number in a box.
        int numInBox(int x, int y, int num)
        {
            int x2,y2,c;

            x = ((x / 3) * 3) + 1;
            y = ((y / 3) * 3) + 1;
            c = 0;
            for (y2 = y - 1; y2 <= y + 1; y2++)
            {
                for (x2 = x - 1; x2 <= x + 1; x2++)
                {
                    if (getNum(x2,y2) == num) c++;
                }
            }
            return c;
        }

        // Can given number be placed at x, y?
        bool placeOK(int x, int y, int num)
        {
            if (getNum(x,y) > 0) return false;
            if (numInRow(y,num) > 0) return false;
            if (numInCol(x,num) > 0) return false;
            if (numInBox(x,y,num) > 0) return false;
            return true;
        }

        // Is this a valid state?
        bool isValid()
        {
            int x,y,i;

            for (y = 0; y < 9; y++)
            {
                for (x = 0; x < 9; x++)
                {
                    i = getNum(x,y);
                    if (i < 0 || i > 9) return false;
                    for (i = 1; i <= 9; i++)
                    {
                        if (numInRow(y,i) > 1) return false;
                        if (numInCol(x,i) > 1) return false;
                        if (numInBox(x,y,i) > 1) return false;
                    }
                }
            }
            return true;
        }

        // Is given state a duplicate of this?
        bool isDuplicate(SudokuState *state)
        {
            for (int i = 0; i < 81; i++)
            {
                if (state->grid[i] != grid[i]) return false;
            }
            return true;
        }

        // Get state value.
        double getValue() { return value; }

        // Set state value.
        void setValue(double value) { this->value = value; }

        // Clone.
        SudokuState *clone()
        {
            SudokuState *state = new SudokuState();
            assert(state != NULL);
            for (int i = 0; i < 81; i++) state->grid[i] = grid[i];
            return state;
        }

        // Load from file.
        void load(char *filename)
        {
            FILE *fp;
            if ((fp = fopen(filename, "r")) == NULL)
            {
                fprintf(stderr, "load: cannot open file %s\n", filename);
                exit(1);
            }
            load(fp);
            fclose(fp);
        }

        // Load file.
        // Empty cells filled with 0.
        void load(FILE *fp=stdin)
        {
            int x,y,n;
            char buf[50];

            for (y = 0; y < 9; y++)
            {
                for (x = 0; x < 49; x++) buf[x] = '0';
                buf[49] = '\0';
                if (fgets(buf, 49, fp) == NULL)
                {
                    fprintf(stderr, "Error loading file\n");
                    exit(1);
                }
                for (x = 0; x < 9; x++)
                {
                    if (buf[x] < '1' || buf[x] > '9')
                    {
                        n = 0;
                    }
                    else
                    {
                        n = buf[x] - '0';
                    }
                    setNum(x,y,n);
                }
            }
        }

        // Save to file.
        void save(char *filename)
        {
            FILE *fp;
            if ((fp = fopen(filename, "w")) == NULL)
            {
                fprintf(stderr, "save: cannot open file %s\n", filename);
                exit(1);
            }
            save(fp);
            fclose(fp);
        }

        void save (FILE *fp=stdout)
        {
            int x,y,n;

            for (y = 0; y < 9; y++)
            {
                for (x = 0; x < 9; x++)
                {
                    n = getNum(x,y);
                    if (n == 0)
                    {
                        fprintf(fp, " ");
                    }
                    else
                    {
                        fprintf(fp, "%d", n);
                    }
                }
                fprintf(fp, "\n");
            }
        }

        // Print.
        void print()
        {
            int x,y,n;

            for (x = 0; x < 19; x++) printf("-"); printf("\n");
            for (y = 0; y < 9; y++)
            {
                printf("|");
                for (x = 0; x < 9; x++)
                {
                    n = getNum(x,y);
                    if (n > 0)
                    {
                        printf("%d|", n);
                    }
                    else
                    {
                        printf(" |");
                    }
                }
                printf("\n");
                for (x = 0; x < 19; x++) printf("-"); printf("\n");
            }
        }

        // Number grid.
        char grid[81];

        // Evaluated value.
        double value;
};
#endif

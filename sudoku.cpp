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

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <list>
#include "sudokuState.hpp"

// Search strategy.
enum { DEPTH, BREADTH, BEST }
Strategy;

// Prevent repeated states?
bool RepeatCheck;

// Open list.
std::list<SudokuState *> OpenList;

// Closed list.
std::list<SudokuState *> ClosedList;

// Count of expanded states.
int ExpandCount;

// Grid of possible values.
bool Maybe[9][9][10];

// Functions.
SudokuState *search();
void deduce(SudokuState *);
bool boxCheck(int, int, int);
bool columnCheck(int, int, int, int);
bool rowCheck(int, int, int, int);
void merge(SudokuState *);
bool repeat(SudokuState *);

// Command-line options.
char *Usage =
"sudoku -loadfile <initial input file> [-savefile <solution output file>]\n\t-strategy <depth | breadth | best> -repeatcheck <true | false>\n";

int
main(int argc, char *argv[])
{
    int i;
    char loadfile[50],savefile[50],buf[50];
    bool gotStrategy,gotRepeatCheck,commandlineUse;

    loadfile[0] = savefile[0] = '\0';
    gotStrategy = gotRepeatCheck = false;
    commandlineUse = true;

    for (i = 1; i < argc; i++)
    {
        if (strcmp(argv[i], "-loadfile") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr,Usage);
                return 1;
            }
            strncpy(loadfile,argv[i],49);
            continue;
        }

        if (strcmp(argv[i], "-savefile") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr,Usage);
                return 1;
            }
            strncpy(savefile,argv[i],49);
            continue;
        }

        if (strcmp(argv[i], "-strategy") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr,Usage);
                return 1;
            }
            if (strcmp(argv[i], "depth") == 0) Strategy = DEPTH;
            else if (strcmp(argv[i], "breadth") == 0) Strategy = BREADTH;
            else if (strcmp(argv[i], "best") == 0) Strategy = BEST;
            else
            {
                fprintf(stderr, "Invalid search strategy option\n");
                fprintf(stderr,Usage);
                return 1;
            }
            gotStrategy = true;
            continue;
        }

        if (strcmp(argv[i], "-repeatcheck") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr,Usage);
                return 1;
            }
            if (strcmp(argv[i], "true") == 0) RepeatCheck = true;
            else if (strcmp(argv[i], "false") == 0) RepeatCheck = false;
            else
            {
                fprintf(stderr, "Invalid repeatcheck option\n");
                fprintf(stderr,Usage);
                return 1;
            }
            gotRepeatCheck = true;
            continue;
        }

        fprintf(stderr,Usage);
        return 1;
    }

    // Get missing input.
    if (loadfile[0] == '\0')
    {
        commandlineUse = false;
        printf("Enter the puzzle load file name: ");
        scanf("%s", loadfile);
    }
    if (!gotStrategy)
    {
        commandlineUse = false;
        printf("Enter search strategy (depth, breadth, best): ");
        scanf("%s", buf);
        if (strcmp(buf, "depth") == 0) Strategy = DEPTH;
        else if (strcmp(buf, "breadth") == 0) Strategy = BREADTH;
        else if (strcmp(buf, "best") == 0) Strategy = BEST;
        else
        {
            fprintf(stderr, "Invalid search strategy\n");
            return 1;
        }
        gotStrategy = true;
    }
    if (!gotRepeatCheck)
    {
        commandlineUse = false;
        printf("Prevent repeating states (y|n)?: ");
        buf[0] = '\0';
        scanf("%s", buf);
        if (buf[0] == 'Y' || buf[0] == 'y')
        {
            RepeatCheck = true;
        }
        else
        {
            RepeatCheck = false;
        }
        gotRepeatCheck = true;
    }

    // Load initial state.
    SudokuState *state = new SudokuState();
    assert(state != NULL);
    state->load(loadfile);

    // Check the initial state.
    if (!state->isValid())
    {
        fprintf(stderr, "Invalid initial state\n");
        return 1;
    }

    // Search for solution.
    printf("Initial puzzle:\n");
    state->print();
    OpenList.push_front(state);
    if ((state = search()) != NULL)
    {
        printf("Found solution!\n");
        state->print();
        printf("%d states expanded\n", ExpandCount);
        if (!commandlineUse && savefile[0] == '\0')
        {
            printf("Save solution to file (y|n)?: ");
            scanf("%s", buf);
            if (buf[0] == 'Y' || buf[0] == 'y')
            {
                printf("Enter file name: ");
                scanf("%s", savefile);
            }
        }
        if (savefile[0] != '\0')
        {
            state->save(savefile);
        }
    }
    else
    {
        printf("No solution!\n");
        printf("%d states expanded\n", ExpandCount);
    }
    return 0;
}


// Search
SudokuState *search()
{
    int x,y,i,c;
    SudokuState *state, *child;

    // Get initial state.
    ExpandCount = 0;
    if (OpenList.size() == 0) return NULL;
    state = *(OpenList.begin());
    OpenList.pop_front();

    // Deduce numbers.
    deduce(state);

    // Check for solution.
    if (state->gridCount() == 81) return state;

    // While there are states to explore.
    while (true)
    {
        // Increment expansion count.
        ExpandCount++;

        // Put on closed list.
        ClosedList.push_back(state);

        // Expand the state.
        for (y = 0; y < 9; y++)
        {
            for (x = 0; x < 9; x++)
            {
                // Count number of possible choices for this cell.
                for (i = 1, c = 0; i <= 9; i++)
                {
                    if (state->placeOK(x,y,i)) c++;
                }

                for (i = 1; i <= 9; i++)
                {
                    if (state->placeOK(x,y,i))
                    {
                        child = state->clone();
                        child->setNum(x,y,i);
						
                        // Deduce numbers.
                        deduce(child);

                        // Check for solution.
                        if (child->gridCount() == 81) return child;

                        // Children with fewer choices are more valuable.
                        child->setValue(((double)child->gridCount() * 10.0) + (double)(9 - c));

                        // Check for repeat and put on open list.
                        if (repeat(child))
                        {
                            delete child;
                        }
                        else
                        {
                            merge(child);
                        }
                    }
                }
            }
        }

        // Get next state to expand.
        if (OpenList.size() == 0) return NULL;
        state = *(OpenList.begin());
        OpenList.pop_front();
    }
}


// Deduce missing numbers in grid.
void deduce(SudokuState *state)
{
    int x,y,z,i,c;

    // Deduce and fill in all possible numbers.
    bool done = false;
    while (!done)
    {
        done = true;

        // Fill in the initial Maybe values.
        for (x = 0; x < 9; x++)
        {
            for (y = 0; y < 9; y++)
            {
                for (z = 0; z < 10; z++)
                {
                    Maybe[x][y][z] = false;
                }
                for (z = 1; z < 10; z++)
                {
                    if (z == state->getNum(x,y))
                    {
                        Maybe[x][y][z] = true;
                    }
                    else
                    {
                        if (state->placeOK(x,y,z))
                        {
                            Maybe[x][y][z] = true;
                        }
                    }
                }
            }
        }

        // Eliminate incompatible Maybe values.
        for (x = 0; x < 9; x++)
        {
            for (y = 0; y < 9; y++)
            {
                if (state->getNum(x,y) > 0) continue;
                for (z = 1; z < 10; z++)
                {
                    if (Maybe[x][y][z])
                    {
                        Maybe[x][y][z] = boxCheck(x,y,z);
                    }
                }
            }
        }

        // Lone Maybe values become new grid values.
        for (x = 0; x < 9 && done; x++)
        {
            for (y = 0; y < 9 && done; y++)
            {
                if (state->getNum(x,y) > 0) continue;
                for (z = 1, c = i = 0; z < 10; z++)
                {
                    if (Maybe[x][y][z])
                    {
                        c++;
                        i = z;
                    }
                }
                if (c == 1)
                {
                    state->setNum(x,y,i);
                    done = false;
                }
            }
        }
    }
}


// Check given number against other boxes.
bool boxCheck(int x, int y, int num)
{
    int cx,cy,cx2,cy2;

    // Find center of this box.
    cx = ((x / 3) * 3) + 1;
    cy = ((y / 3) * 3) + 1;

    // Check columns in boxes above and below.
    // If given number must be in the same column
    // then there is no possibility of the number
    // being at the given position.
    cx2 = cx;
    cy2 = cy - 6;
    if (cy2 >= 0 && !columnCheck(x,cx2,cy2,num)) return false;
    cy2 = cy - 3;
    if (cy2 >= 0 && !columnCheck(x,cx2,cy2,num)) return false;
    cy2 = cy + 6;
    if (cy2 < 9 && !columnCheck(x,cx2,cy2,num)) return false;
    cy2 = cy + 3;
    if (cy2 < 9 && !columnCheck(x,cx2,cy2,num)) return false;

    // Check rows in boxes left and right.
    // If given number must be in the same row
    // then there is no possibility of the number
    // being at the given position.
    cx2 = cx - 6;
    cy2 = cy;
    if (cx2 >= 0 && !rowCheck(y,cx2,cy2,num)) return false;
    cx2 = cx - 3;
    if (cx2 >= 0 && !rowCheck(y,cx2,cy2,num)) return false;
    cx2 = cx + 6;
    if (cx2 < 9 && !rowCheck(y,cx2,cy2,num)) return false;
    cx2 = cx + 3;
    if (cx2 < 9 && !rowCheck(y,cx2,cy2,num)) return false;
    return true;
}


// Check if number can be in box other than in given column.
bool columnCheck(int col, int cx, int cy, int num)
{
    int x,y;

    for (y = cy - 1; y <= cy + 1; y++)
    {
        for (x = cx - 1; x <= cx + 1; x++)
        {
            if (x != col && Maybe[x][y][num]) return true;
        }
    }

    return false;
}


// Check if number can be in box other than in given row.
bool rowCheck(int row, int cx, int cy, int num)
{
    int x,y;

    for (y = cy - 1; y <= cy + 1; y++)
    {
        for (x = cx - 1; x <= cx + 1; x++)
        {
            if (y != row && Maybe[x][y][num]) return true;
        }
    }

    return false;
}


// Merge state into open list.
void merge(SudokuState *state)
{
    std::list<SudokuState *>::iterator listItr;

    switch(Strategy)
    {
        case DEPTH:
            OpenList.push_front(state);
            return;
        case BREADTH:
            OpenList.push_back(state);
            return;
        case BEST:
            for (listItr = OpenList.begin();
                listItr != OpenList.end(); listItr++)
            {
                if (state->getValue() >= (*listItr)->getValue())
                {
                    OpenList.insert(listItr, state);
                    return;
                }
            }
            OpenList.push_back(state);
            return;
        default: assert(false);
    }
}


// State is repeating?
bool repeat(SudokuState *state)
{
    std::list<SudokuState *>::iterator listItr;

    if (!RepeatCheck) return false;

    for (listItr = ClosedList.begin();
        listItr != ClosedList.end(); listItr++)
    {
        if (state->isDuplicate(*listItr)) return true;
    }
    return false;
}

package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;

import java.util.*;

public class Main extends EngineFrame {

    private static final int SIZE = 3;
    private static final int MAX_RECURSION_DEPTH = 10000;

    private Piece[][] grid;
    private double pieceSize;
    private Image pieceImage;

    private boolean stopSolving;
    private int currentRecursionDepth;

    private Set<String> visitedStates;
    private Deque<Vector2> solutionMoves;

    public Main() {
        
        super(
            600,                 // largura                      / width
            600,                 // algura                       / height
            "Quebra-cabeça",      // título                       / title
            60,                  // quadros por segundo desejado / target FPS
            true,                // suavização                   / antialiasing
            false,               // redimensionável              / resizable
            false,               // tela cheia                   / full screen
            false,               // sem decoração                / undecorated
            false,               // sempre no topo               / always on top
            false                // fundo invisível              / invisible background
        );
        
    }

    @Override
    public void create() {

        grid = new Piece[SIZE][SIZE];
        pieceSize = (double) getScreenHeight() / SIZE;
        pieceImage = loadImage("resources/images/gato.jpg").resize(getScreenWidth());

        for (int i = 0; i < SIZE; i++ ) {
            for (int j = 0; j < SIZE; j++ ) {
                grid[i][j] = new Piece(
                        j * pieceSize,
                        i * pieceSize,
                        pieceSize,
                        i * SIZE + j,
                        pieceImage
                );
            }
        }

        grid[SIZE - 1][SIZE - 1] = null;

        visitedStates = new HashSet<>();
        solutionMoves = new LinkedList<>();

    }

    @Override
    public void update( double delta ) {

        if ( isMouseButtonPressed(MOUSE_BUTTON_LEFT) ) {
            for (int i = 0; i < SIZE; i++ ) {
                for (int j = 0; j < SIZE; j++ ) {
                    if ( grid[i][j] != null && grid[i][j].intercept(getMouseX(), getMouseY())) {
                        movePiece( i, j );
                    }
                }
            }
        }

        if ( isKeyPressed(KEY_S) ) {
            shuffle( 9 );
        }

        if ( isKeyPressed(KEY_SPACE) ) {
            while ( !checkFinished() ) {
                try {
                    solve();
                    if ( checkFinished() ) System.out.println("finished");
                } catch (IllegalStateException e) {
                    shuffle( SIZE * SIZE );
                    System.out.println(e.getMessage());
                }
            }
        }

    }

    @Override
    public void draw() {
        
        clearBackground( WHITE );

       for (int i = 0; i < SIZE; i++ ) {
           for (int j = 0; j < SIZE; j++ ) {
               if ( grid[i][j] != null ) {
                   grid[i][j].draw(this, SIZE);
               }
           }
       }

        drawFPS( 20, 20 );
    
    }

    private void movePiece(int row, int column ) {

        int[] rowNeighbors = { -1, 0, 1, 0 };
        int[] columnNeighbors = { 0, 1, 0, -1 };

        int rowDestination = -1;
        int columnDestination = -1;

        for ( int i = 0; i < 4; i++ ) {
            int currentRow = row + rowNeighbors[i];
            int currentColumn = column + columnNeighbors[i];
            if ( currentRow >= 0 && currentColumn >= 0 && currentRow < SIZE && currentColumn < SIZE) {
                if ( grid[currentRow][currentColumn] == null ) {
                    rowDestination = currentRow;
                    columnDestination = currentColumn;
                    break;
                }
            }
        }

        if ( rowDestination != -1 ) {
            grid[rowDestination][columnDestination] = grid[row][column];
            grid[row][column] = null;
            recalculatePositions();
        }

    }

    private void recalculatePositions() {
        for (int i = 0; i < SIZE; i++ ) {
            for (int j = 0; j < SIZE; j++ ) {
                if ( grid[i][j] != null) {
                    grid[i][j].setPos( j * pieceSize, i * pieceSize);
                }
            }
        }
    }

    private void shuffle( int count ) {

        int[] rowNeighbors = { -1, 0, 1, 0 };
        int[] columnNeighbors = { 0, 1, 0, -1 };

        for ( int i = 0; i < count; i++ ) {
            Vector2 emptyPosition = getEmptyPosition();

            while (true) {
                int possibleDestination = MathUtils.getRandomValue(0, 100) % 4;
                int currentRow = (int) emptyPosition.y + rowNeighbors[possibleDestination];
                int currentColumn = (int) emptyPosition.x + columnNeighbors[possibleDestination];
                if ( currentRow >= 0 && currentRow < SIZE &&  currentColumn >= 0 && currentColumn < SIZE) {
                    movePiece( currentRow, currentColumn );
                    recalculatePositions();
                    break;
                }
            }

        }

        // garante que a peça vazia sempre esteja na ultima linha e na ultima coluna
        while (true) {
            Vector2 emptyPosition = getEmptyPosition();
            if ( emptyPosition.x != SIZE - 1 ) {
                movePiece((int) emptyPosition.y, (int) emptyPosition.x + 1);
            } else if ( emptyPosition.y != SIZE - 1 ) {
                movePiece((int) emptyPosition.y + 1, (int) emptyPosition.x);
            } else {
                break;
            }
        }
    }

    private Vector2 getEmptyPosition() {
        for (int i = 0; i < SIZE; i++ ) {
            for (int j = 0; j < SIZE; j++ ) {
                if ( grid[i][j] == null ) {
                    return new Vector2(
                            j,
                            i
                    );
                }
            }
        }

        return null;
    }

    private void solve() throws IllegalStateException {

        stopSolving = false;
        currentRecursionDepth = 0;
        visitedStates.clear();
        solutionMoves.clear();

        // stores the initial state
        String initialState = getCurrentBoardState();
        visitedStates.add( initialState );

        // get the initial moving candidates
        List<Vector2> candidates = getCandidatesToMove();

        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                stopSolving = true;
                break;
            }
        }

    }

    private List<Vector2> getCandidatesToMove() {

        List<Vector2> candidates = new ArrayList<>();
        Vector2 emptyPos = getEmptyPosition();

        int[] rowNeighbors = { -1, 0, 1, 0 };
        int[] columnNeighbors = { 0, 1, 0, -1 };

        for ( int i = 0; i < 4; i++ ) {
            int currentRow = (int) emptyPos.y + rowNeighbors[i];
            int currentColumn = (int) emptyPos.x + columnNeighbors[i];
            if ( currentRow >= 0 && currentColumn >= 0 && currentRow < SIZE && currentColumn < SIZE) {
                candidates.add( new Vector2( currentColumn, currentRow ) );
            }
        }

        return candidates;

    }

    private String getCurrentBoardState() {
        String currentState = "";
        for ( int i = 0; i < SIZE; i++ ) {
            for  ( int j = 0; j < SIZE; j++ ) {
                if ( grid[i][j] != null ) {
                    currentState += grid[i][j].getValue();
                } else {
                    currentState += "-1";
                }
            }
        }
        return currentState;
    }

    // perform the solve algorithm using backtracking based in a position
    private boolean solveRecurive( Vector2 pos ) throws IllegalStateException {

        currentRecursionDepth++;

        if ( currentRecursionDepth > MAX_RECURSION_DEPTH ) {
            stopSolving = true;
            throw new IllegalStateException( "reached max recursion depth!" );
        }

        // stop trying to solve
        if ( stopSolving ) {
            return false;
        }

        // for backtracking, stores the backward movement before starting
        Vector2 backward = getEmptyPosition();

        // move the current piece to the empty space
        movePiece( (int) pos.y, (int) pos.x );

        // adds to the solutions (maybe will need to remove)
        solutionMoves.addLast( pos );

        // get the current state
        String currentState = getCurrentBoardState();

        // this state was already processed?
        if ( visitedStates.contains( currentState ) ) {

            // undo movement
            movePiece( (int) backward.y, (int) backward.x );

            // remove the move, because it is not correct
            solutionMoves.removeLast();

            // stop the current solution try
            return false;

        }

        // ok, this movement shows promise!

        // add the state
        visitedStates.add( currentState );

        // checks solution
        if ( checkFinished() ) {
            // solution found, stop and signals other calls to stop
            stopSolving = true;
            return true;
        }

        // recursion, trying to find the solution in subproblems
        List<Vector2> candidates = getCandidatesToMove();
        for ( Vector2 c : candidates ) {
            if ( solveRecurive( c ) ) {
                return true; // solution found in a subproblem
            }
        }

        // no solution found in subproblems, so the current path for
        // problem solving is incorret
        movePiece( (int) backward.y, (int) backward.x );
        //visitedStates.remove( currentState );
        solutionMoves.removeLast();

        currentRecursionDepth--;

        // theres no path from here
        return false;

    }

    private boolean checkFinished() {
        return getCurrentBoardState().equals("01234567-1");
    }

    public static void main( String[] args ) {
        new Main();
    }
    
}

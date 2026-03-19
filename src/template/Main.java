package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiComponent;
import br.com.davidbuzatto.jsge.imgui.GuiLabel;
import br.com.davidbuzatto.jsge.imgui.GuiTextComponent;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Main extends EngineFrame {

    private enum GameState {
        MOVING_PIECE,
        DEFAULT,
        RUNNING_SOLUTION
    }

    private static final int SIZE = 3;
    private static final int MAX_RECURSION_DEPTH = 10000;

    private Piece[][] grid;
    private Piece[][] savedGrid;
    private double pieceSize;
    private Image pieceImage;

    private boolean stopSolving;
    private int currentRecursionDepth;
    private int totalMovesToSolve;
    private int currentMoveNumber;
    private int numberOfReshuffle;

    private Set<String> statesAlreadyVisited;
    private Deque<Vector2> movesToSolve;

    private GameState gameState;
    private Vector2 currentPosition;
    private Vector2 targetPosition;
    private double movementPercentage;
    private Piece movingPiece;
    private double movementSpeed;

    private List<GuiComponent> components;

    private GuiButton trySolveButton;
    private GuiButton shuffleButton;
    private GuiLabel numberOfReshuffleLabel;

    private Color backgroundColor;
    private Color backgroundColorComponents;
    private Color textColorComponents;
    private Color borderColorComponents;

    public Main() {
        
        super(
            1200,                 // largura                      / width
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
        savedGrid = new  Piece[SIZE][SIZE];
        pieceSize = (double) getScreenHeight() / SIZE;
        pieceImage = loadImage("resources/images/gato.jpg").resize(getScreenHeight());

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

        statesAlreadyVisited = new HashSet<>();
        movesToSolve = new LinkedList<>();

        gameState = GameState.DEFAULT;
        movementSpeed = 10;

        double buttonWidth = 500;
        double buttonHeight = 40;
        double xButton = (3.0 / 4) * getScreenWidth() - buttonWidth / 2;
        double yGap = buttonHeight + 20;
        double numberOfButtons = 3;
        double yButton = ( getScreenHeight() - buttonHeight ) / 2 - ( numberOfButtons - 1 ) * yGap / 2 ;

        trySolveButton = new GuiButton(
                xButton,
                yButton,
                buttonWidth,
                buttonHeight,
                "Try to solve",
                this
        );

        shuffleButton = new GuiButton(
                xButton,
                yButton + yGap,
                buttonWidth,
                buttonHeight,
                "Shuffle",
                this
        );

        numberOfReshuffleLabel = new GuiLabel(
                xButton,
                yButton + 2 * yGap,
                buttonWidth,
                buttonHeight,
                "",
                this
        );

        components = new ArrayList<>();

        components.add(trySolveButton);
        components.add(shuffleButton);
        components.add(numberOfReshuffleLabel);

        backgroundColor = Color.decode("#355872");
        backgroundColorComponents = Color.decode("#7AAACE");
        textColorComponents = Color.decode("#F7F8F0");
        borderColorComponents = Color.decode("#F7F8F0");

        for ( GuiComponent component : components ) {
            component.setBackgroundColor(backgroundColorComponents);
            component.setBorderColor(borderColorComponents);
            component.setTextColor(textColorComponents);
            if ( component instanceof GuiLabel label ) {
                label.setHorizontalAlignment(GuiTextComponent.CENTER_ALIGNMENT);
            }

        }

    }

    @Override
    public void update( double delta ) {

        for ( GuiComponent component : components ) {
            component.update( delta );
        }

        if ( gameState == GameState.MOVING_PIECE ) {
            movementPercentage += movementSpeed * delta;
            movingPiece.setPos(
                    currentPosition.x + ( targetPosition.x - currentPosition.x ) * movementPercentage,
                    currentPosition.y + ( targetPosition.y - currentPosition.y ) * movementPercentage
            );
            if ( movementPercentage > 1 ) {
                gameState = GameState.DEFAULT;
                recalculatePositions();
            }
        } else if ( gameState == GameState.RUNNING_SOLUTION ) {
            trySolveButton.setEnabled( false );
            shuffleButton.setEnabled( false );
            if  ( !movesToSolve.isEmpty() ) {
                Vector2 currentPiece = movesToSolve.removeFirst();
                movePiece( (int) currentPiece.y, (int) currentPiece.x );
                currentMoveNumber++;
            } else {
                gameState = GameState.DEFAULT;
            }
        } else if ( gameState == GameState.DEFAULT ) {
            trySolveButton.setEnabled( true );
            shuffleButton.setEnabled( true );
        }

        if ( isMouseButtonPressed(MOUSE_BUTTON_LEFT) ) {
            for (int i = 0; i < SIZE; i++ ) {
                for (int j = 0; j < SIZE; j++ ) {
                    if ( grid[i][j] != null && gameState != GameState.MOVING_PIECE && grid[i][j].intercept(getMouseX(), getMouseY())) {
                        movePieceWithAnimation( i, j );
                    }
                }
            }
        }

        if ( shuffleButton.isMousePressed() ) {
            shuffle( 200 );
        }

        if ( trySolveButton.isMousePressed() && !isItFinished() ) {
            numberOfReshuffle = 0;
            while ( !isItFinished() ) {
                try {
                    saveState();
                    solve();
                } catch (IllegalStateException e) {
                    shuffle( SIZE * SIZE );
                    numberOfReshuffle++;
                }
            }
            loadState();
            gameState = GameState.RUNNING_SOLUTION;
            totalMovesToSolve = movesToSolve.size();
            numberOfReshuffleLabel.setText("Number of reshuffles: "  +  numberOfReshuffle);
            currentMoveNumber = 0;
        }

    }

    @Override
    public void draw() {
        
        clearBackground( backgroundColor );

        for  ( GuiComponent component : components ) {
            component.draw();
        }

       for (int i = 0; i < SIZE; i++ ) {
           for (int j = 0; j < SIZE; j++ ) {
               if ( grid[i][j] != null ) {
                   grid[i][j].draw(this, SIZE);
               }
           }
       }

       drawLine( getScreenWidth() / 2, 0, getScreenWidth() / 2, getScreenHeight(), BLACK );

       drawText( String.format("%d/%d", currentMoveNumber, totalMovesToSolve), 20, 20, 20, WHITE );
    
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

    private void movePieceWithAnimation(int row, int column ) {

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
            gameState = GameState.MOVING_PIECE;

            currentPosition = new Vector2(
                    column * pieceSize,
                    row * pieceSize
            );

            targetPosition = new  Vector2(
                    columnDestination *  pieceSize,
                    rowDestination * pieceSize
            );

            movementPercentage = 0;

            movingPiece = grid[rowDestination][columnDestination];
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

        // reset the variables
        stopSolving = false;
        currentRecursionDepth = 0;

        // clear the data structures
        statesAlreadyVisited.clear();
        movesToSolve.clear();

        // stores the initial state
        statesAlreadyVisited.add( getCurrentBoardState() );

        // attempts to resolve the problem by considering all possible moves by the candidates
        for ( Vector2 c : getCandidatesToMove() ) {
            if ( solve( c ) ) {
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

    /*
    * To call this method you need to take all the candidates for the move and test
    * them against a given candidate, and you need to update the "stop solving"
    * variable if the puzzle has been solved
    */

    private boolean solve( Vector2 position ) {

        // increases the recursion level variable
        currentRecursionDepth++;

        // check if you've reached the maximum recursion level
        if ( currentRecursionDepth > MAX_RECURSION_DEPTH ) {
            // stop solving
            stopSolving = true;
            // throw exception
            throw new IllegalStateException( "reached max recursion depth!" );
        }

        // if stop solving
        if ( stopSolving ) {
            // return false
            return false;
        }

        // save the location of the empty space before moving it
        Vector2 previousEmptyPosition = getEmptyPosition();

        // move the part to the empty position
        movePiece( (int) position.y, (int) position.x );

        // add this movement to the set of solutions
        movesToSolve.addLast( position );

        // check if this state has already occurred
        if ( statesAlreadyVisited.contains( getCurrentBoardState() ) ) {
            // undo that move
            movePiece( (int) previousEmptyPosition.y, (int) previousEmptyPosition.x );
            // remove this action from the set of solutions
            movesToSolve.removeLast();
            // return false
            return false;
        }

        // add this state to the states already visited
        statesAlreadyVisited.add( getCurrentBoardState() );

        // check if the puzzle has been solved
        if ( isItFinished() ) {
            // stop solving
            stopSolving = true;
            // return true
            return true;
        }

        // here comes the recursion, it is necessary to test each candidate to move
        for ( Vector2 candidate : getCandidatesToMove() ) {
            // return true if its solve
            if ( solve( candidate ) ) {
                return true;
            }
        }

        // no solution found, so go back (move piece to back)
        movePiece( (int) previousEmptyPosition.y, (int) previousEmptyPosition.x );

        // remove this movement from the list of solutions
        movesToSolve.removeLast();

        // decrements the recursion level variable
        currentRecursionDepth--;

        // return false
        return false;

    }

    private void saveState() {
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( grid[i][j] != null ) {
                    savedGrid[i][j] = grid[i][j].copy();
                } else {
                    savedGrid[i][j] = null;
                }
            }
        }
    }

    private void loadState() {
        for ( int i = 0; i < SIZE; i++ ) {
            for ( int j = 0; j < SIZE; j++ ) {
                if ( savedGrid[i][j] != null ) {
                    grid[i][j] = savedGrid[i][j].copy();
                } else {
                    grid[i][j] = null;
                }
            }
        }
    }

    // this method, as it stands, is not suitable for boards of any size
    private boolean isItFinished() {
        return getCurrentBoardState().equals("01234567-1");
    }

    public static void main( String[] args ) {
        new Main();
    }
    
}

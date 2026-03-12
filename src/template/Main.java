package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;

public class Main extends EngineFrame {

    private enum GameState {

    }

    private static final int SIZE = 3;
    private Peca[][] grid;
    private double pieceSize;
    private Image pieceImage;

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

        grid = new Peca[SIZE][SIZE];
        pieceSize = (double) getScreenHeight() / SIZE;
        pieceImage = loadImage("resources/images/gato.jpg").resize(getScreenWidth());

        for (int i = 0; i < SIZE; i++ ) {
            for (int j = 0; j < SIZE; j++ ) {
                grid[i][j] = new Peca(
                        j * pieceSize,
                        i * pieceSize,
                        pieceSize,
                        i * SIZE + j,
                        pieceImage
                );
            }
        }

        grid[SIZE - 1][SIZE - 1] = null;

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
            shuffle( 200 );
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

    public static void main( String[] args ) {
        new Main();
    }
    
}

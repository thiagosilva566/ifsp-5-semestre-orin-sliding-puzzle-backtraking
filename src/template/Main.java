package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.MathUtils;
import br.com.davidbuzatto.jsge.math.Vector2;

public class Main extends EngineFrame {

    private static final int SIZE = 3;
    private Piece[][] grid;
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

    /*
    // para chamar esse metodo é necessario pegar todos os candidatos ao movimento
    // e testar com dada candidato, e é necessario atualizar a variavel de stop solving
    // caso o quebra cabeça tenha sido resolvido
    */

    // this method should return a boolean
    private void solve( Vector2 position ) {

        // incrementa a variável de nível da recursão

        // checa se ja chegou no nivel maximo de recursao

            // stop solving
            // throw exception

        // if stop solving

            // return false

        // save the location of the empty space before moving it

        // move the part to the empty position

        // add this movement to the set of solutions

        // save the current game state

        // check if this state has already occurred

            // desfazer esse movimento

            // remover esse movimento do conjunto de soluções

            // return false

        // add this state to the states already visited

        // check if the puzzle has been solved

            // stop solving
            // return true

        // here comes the recursion, it is necessary to test each candidate to move

            // return true if its solve

        // no solution found, so go back (move piece to back)

        // remove this movement from the list of solutions

        // return false

    }

    public static void main( String[] args ) {
        new Main();
    }
    
}

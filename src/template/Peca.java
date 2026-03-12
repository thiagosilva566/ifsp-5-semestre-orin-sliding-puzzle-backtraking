package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.geom.Rectangle;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.Vector2;

public class Peca {

    private Vector2 pos;
    private double size;
    private int value;
    private Image image;

    public Peca(double x, double y, double size, int value, Image image) {
        this.pos = new Vector2( x, y );
        this.size = size;
        this.value = value;
        this.image = image;
    }

    public void draw( EngineFrame e, int gridSize ) {

        e.drawImage(image,
                new Rectangle(
                        value % gridSize * size,
                        value / gridSize * size,
                        size,
                        size
                ),
                new Rectangle( pos.x, pos.y, size, size),
                EngineFrame.WHITE
        );
        e.drawRectangle( pos, size, size, EngineFrame.BLACK );
    }

    public boolean intercept(double x, double y ) {
        return x >= pos.x && x <= pos.x + size &&
                y >= pos.y && y <= pos.y + size;
    }

    public int getValue() {
        return value;
    }

    public void setPos( double x, double y ) {
        this.pos = new Vector2(x, y);
    }
}

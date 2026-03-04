import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import game2D.*;

// Game demonstrates how we can override the GameCore class
// Student ID: 3359480

@SuppressWarnings("serial")
public class Game extends GameCore 
{
	// Useful game constants
	static int screenWidth = 512;
	static int screenHeight = 384;

	// Game constants
    float 	lift = 0.005f;
    float	gravity = 0.0001f;
    float	fly = -0.04f;
    float	moveSpeed = 0.05f;
    
    // Game state flags
    boolean moveRight = false;
    boolean moveLeft = false;
    boolean onGround = false;
    boolean jumpPressed = false;
    boolean debug = true;		

    // Game resources
    Animation landing, walk, walkR;
    
    Sprite	player = null;
    ArrayList<Sprite> 	clouds = new ArrayList<Sprite>();
    ArrayList<Tile>		collidedTiles = new ArrayList<Tile>();

    TileMap tmap = new TileMap();	// Our tile map
    
    long total;

    public static void main(String[] args) {
        Game gct = new Game();
        gct.init();
        gct.run(false,screenWidth,screenHeight);
    }

    public void init()
    {         
        Sprite s;
        tmap.loadMap("maps", "level1.txt");
        
        setSize(tmap.getPixelWidth()/4, tmap.getPixelHeight());
        setVisible(true);
        
        landing = new Animation();
        landing.loadAnimationFromSheet("images/Lidle.png", 2, 1, 300);

        walk = new Animation();
        walk.loadAnimationFromSheet("images/Lwalk.png", 6, 1, 100);
        
        walkR = new Animation();
        walkR.loadAnimationFromSheet("images/Walk.png", 6, 1, 100);

        player = new Sprite(landing);
        player.setScale(2.0f);
        
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);
        
        for (int c=0; c<3; c++)
        {
        	s = new Sprite(ca);
        	s.setX(screenWidth + (int)(Math.random()*200.0f));
        	s.setY(30 + (int)(Math.random()*150.0f));
        	s.setVelocityX(-0.02f);
        	s.show();
        	clouds.add(s);
        }

        initialiseGame();
    }

    public void initialiseGame()
    {
    	total = 0;
        player.setPosition(200,200);
        player.setVelocity(0,0);
        player.show();
    }
    
    public void draw(Graphics2D g)
    {    	
        int xo = -(int)player.getX() + 200;
        int yo = -(int)player.getY() + 200;

        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        for (Sprite s: clouds)
        {
        	s.setOffsets(xo,yo);
        	s.draw(g);
        }

        tmap.draw(g,xo,yo); 

        player.setOffsets(xo, yo);
        player.draw(g);
        
        if (debug)
        {
            tmap.drawBorder(g, xo, yo, Color.black);
            g.setColor(Color.red);
        	player.drawBoundingBox(g);
        	drawCollidedTiles(g, tmap, xo, yo);
        }
    }

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset)
    {
		if (collidedTiles.size() > 0)
		{	
			int tileWidth = map.getTileWidth();
			int tileHeight = map.getTileHeight();
			g.setColor(Color.blue);
			for (Tile t : collidedTiles)
				g.drawRect(t.getXC()+xOffset, t.getYC()+yOffset, tileWidth, tileHeight);
		}
    }
	
    public void update(long elapsed)
    {
        onGround = false;
        
        // FIXED ERROR: Changed setVelocityY() call inside to getVelocityY()
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
        
        if (moveRight) {
            player.setAnimation(walk); 
            player.setVelocityX(moveSpeed);
            player.setScale(2.0f, 2.0f); 
        } 
        else if (moveLeft) {
            player.setAnimation(walkR); 
            player.setVelocityX(-moveSpeed);
            player.setScale(-2.0f, 2.0f); 
        } 
        else {
            player.setVelocityX(0);
            player.setAnimation(landing);
            player.setScale(2.0f, 2.0f);
        }
        
       	for (Sprite s: clouds) s.update(elapsed);
        player.update(elapsed);
       
        handleScreenEdge(player, tmap, elapsed);
        checkTileCollision(player, tmap);
        
        if (jumpPressed && onGround) {
            player.setVelocityY(-0.2f);
            jumpPressed = false;
        }
    }
    
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed)
    {
    	float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (difference > 0)
        {
        	s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(difference)); 
        	s.setVelocityY(-s.getVelocityY()*0.75f);
        }
    }
    
    public void keyPressed(KeyEvent e) 
    { 
    	int key = e.getKeyCode();
		switch (key)
		{
			case KeyEvent.VK_UP: jumpPressed = true; break;
			case KeyEvent.VK_RIGHT: moveRight = true; break;
			case KeyEvent.VK_LEFT: moveLeft = true; break;
			case KeyEvent.VK_ESCAPE: stop(); System.exit(0); break; // FIXED: Added System.exit to clear icons
			case KeyEvent.VK_B: debug = !debug; break;
		}
    }

    public void checkTileCollision(Sprite s, TileMap tmap)
    {
        collidedTiles.clear();
        float sx = s.getX();
        float sy = s.getY();
        float swidth = s.getWidth() * 0.5f; // Tighter collision box for immediate falling
        float sheight = s.getHeight();
        float offsetX = (s.getWidth() - swidth) / 2;
        
        float tileWidth = tmap.getTileWidth();
        float tileHeight = tmap.getTileHeight();
        
        int xL = (int) ((sx + offsetX) / tileWidth);
        int xR = (int) ((sx + offsetX + swidth - 1) / tileWidth);
        int yB = (int) ((sy + sheight) / tileHeight);
        
        Tile fL = tmap.getTile(xL, yB);
        Tile fR = tmap.getTile(xR, yB);
        
        if (s.getVelocityY() >= 0) {
            if ((fL != null && fL.getCharacter() != '.') || (fR != null && fR.getCharacter() != '.')) {
                Tile t = (fL != null && fL.getCharacter() != '.') ? fL : fR;
                s.setY(t.getYC() - sheight);
                s.setVelocityY(0); 
                onGround = true;
                collidedTiles.add(t);
            }
        }
    }

	public void keyReleased(KeyEvent e) { 
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_UP) jumpPressed = false;
		if (key == KeyEvent.VK_RIGHT) moveRight = false;
		if (key == KeyEvent.VK_LEFT) moveLeft = false;
	}
}
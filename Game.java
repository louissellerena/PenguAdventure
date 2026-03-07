import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. 

// Student ID: 3359480

@SuppressWarnings("serial")

public class Game extends GameCore implements MouseListener
{
	// Useful game constants
    static int screenWidth = 800; 
    static int screenHeight = 600;
    
    // Game constants
    float gravity = 0.0001f;
    float moveSpeed = 0.05f;
    
    // Game state flags
    boolean moveRight = false, moveLeft = false, onGround = false, jumpPressed = false, debug = false;
    boolean isAlive = true;
    boolean showDeathScreen = false; 
    long deathTimer = 0; // Timer to ensure the 1-frame death UI pops up
    
    // Game resources
    Animation landing, walk, walkR, jump, jumpL, death;
    
    // Parallax background layers
    Image skyLayer, mountainLayer, treesBackLayer, treesFrontLayer, groundLayer;
    
    Sprite player = null;
    ArrayList<Sprite> clouds = new ArrayList<Sprite>();
    ArrayList<Tile> collidedTiles = new ArrayList<Tile>();
    
    TileMap tmap = new TileMap(); // Our tile map, note that we load it in init()
    
    long total; // The score will be the total time elapsed since a crash

    // UI Button - Centered on screen
    Rectangle restartBtn = new Rectangle(screenWidth/2 - 50, screenHeight/2 + 20, 100, 40);

    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {
        Game gct = new Game();
        gct.init();
        // Start in windowed mode with the given screen height and width
        gct.run(false, screenWidth, screenHeight);
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers.
     * * This shows you the general principles but you should create specific
     * methods for setting up your game that can be called again when you wish to 
     * restart the game (for example you may only want to load animations once
     * but you could reset the positions of sprites each time you restart the game).
     */
    public void init()
    {          
        Sprite s; // Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "level1.txt");
        
        setSize(screenWidth, screenHeight);
        setVisible(true);
        
        addMouseListener(this);
        
        // Load parallax background layers
        skyLayer = loadImage("images/5.png");
        mountainLayer = loadImage("images/4.png");
        treesBackLayer = loadImage("images/3.png");
        treesFrontLayer = loadImage("images/2.png");
        groundLayer = loadImage("images/1.png");
        
        // Create a set of background sprites that we can 
        // rearrange to give the illusion of motion
        
        landing = new Animation();
        landing.loadAnimationFromSheet("images/Lidle.png", 2, 1, 300);
        
        walk = new Animation();
        walk.loadAnimationFromSheet("images/Lwalk.png", 6, 1, 100);
        
        walkR = new Animation();
        walkR.loadAnimationFromSheet("images/Walk.png", 6, 1, 100);

        jump = new Animation();
        jump.loadAnimationFromSheet("images/Ljump.png", 2, 1, 1000);
        
        jumpL = new Animation();
        jumpL.loadAnimationFromSheet("images/Jump.png", 2, 1, 1000);

        // Death animation - keep to 1,1 for your specific spritesheet
        death = new Animation();
        death.loadAnimationFromSheet("images/Death.png", 1, 1, 150); 
        death.setLoop(false); 

        // Initialise the player with an animation
        player = new Sprite(landing);
        player.setScale(2.0f);
        
        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);
        
        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c=0; c<6; c++) {
            s = new Sprite(ca);
            s.setX((int)(Math.random() * tmap.getPixelWidth()));
            s.setY(50 + (int)(Math.random() * 200.0f));
            s.setVelocityX(-0.01f);
            s.show();
            clouds.add(s);
        }
        
        initialiseGame();
        
        System.out.println(tmap);
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it when restarting
     * the game when the player loses.
     */
    public void initialiseGame() {
        total = 0;
        isAlive = true;
        showDeathScreen = false; 
        deathTimer = 0; // Reset the death timer
        player.setPosition(500, 2700); //starting position of the penguin
        player.setVelocity(0, 0);
        player.setAnimation(landing);
        player.show();
    }
    
    /**
     * Draw the current state of the game. Note the sample use of
     * debugging output that is drawn directly to the game screen.
     */
    public void draw(Graphics2D g) {    
    	// Be careful about the order in which you draw objects - you
    	// should draw the background first, then work your way 'forward'

    	// First work out how much we need to shift the view in order to
    	// see where the player is. To do this, we adjust the offset so that
        // it is relative to the player's position along with a shift
        int xo = -(int)player.getX() + (screenWidth / 2);
        int yo = -(int)player.getY() + (screenHeight / 2);

        // Continuous scrolling logic
        int bgWidth = 800; 

        // Draw parallax background layers with horizontal tiling
        drawLoopingLayer(g, skyLayer, xo, 0.1f, 0, bgWidth);
        drawLoopingLayer(g, mountainLayer, xo, 0.3f, 0, bgWidth);
        drawLoopingLayer(g, treesBackLayer, xo, 0.5f, 0, bgWidth);
        drawLoopingLayer(g, treesFrontLayer, xo, 0.7f, 0, bgWidth);
        drawLoopingLayer(g, groundLayer, xo, 0.9f, 0, bgWidth);
        
        // Apply offsets to sprites then draw them
        for (Sprite s: clouds) {
            s.setOffsets((int)(xo * 0.5f), yo); 
            s.draw(g);
        }

        // Apply offsets to tile map and draw  it
        tmap.draw(g, xo, yo); 

        // Apply offsets to player and draw 
        player.setOffsets(xo, yo);
        player.draw(g);
        
        // Show score and status information
        String scoreMsg = String.format("Score: %d", total/100);
        g.setColor(Color.darkGray);
        g.drawString(scoreMsg, getWidth() - 100, 50);
        
        if (debug) {
        	// When in debug mode, you could draw borders around objects
            // and write messages to the screen with useful information.
            // Try to avoid printing to the console since it will produce 
            // a lot of output and slow down your game.
            g.setColor(Color.red);
            player.drawBoundingBox(g);
            drawCollidedTiles(g, tmap, xo, yo);
        }

        // DRAW UI LAST so it's on top of everything
        if (showDeathScreen) {
            drawDeathUI(g);
        }
    }

    /**
     * Tiling helper to ensure backgrounds are continuous
     */
    private void drawLoopingLayer(Graphics2D g, Image img, int xo, float speed, int y, int width) {
        if (img == null) return;
        int xPos = (int)(xo * speed) % width;
        g.drawImage(img, xPos, y, width, getHeight(), null);
        if (xPos < 0) 
            g.drawImage(img, xPos + width, y, width, getHeight(), null);
        else 
            g.drawImage(img, xPos - width, y, width, getHeight(), null);
    }

    private void drawDeathUI(Graphics2D g) {
        // Overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // Text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        String msg = "YOU DIED";
        int strWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (screenWidth - strWidth) / 2, screenHeight / 2 - 20);

        // Button
        g.setColor(Color.RED);
        g.fill(restartBtn);
        g.setColor(Color.WHITE);
        g.draw(restartBtn);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Restart", restartBtn.x + 18, restartBtn.y + 26);
    }

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
		if (collidedTiles.size() > 0) {   
			int tileWidth = map.getTileWidth();
			int tileHeight = map.getTileHeight();
			
            g.setColor(Color.blue);
            for (Tile t : collidedTiles)
                g.drawRect(t.getXC() + xOffset, t.getYC() + yOffset, tileWidth, tileHeight);
        }
    }
	
    /**
     * Update any sprites and check for collisions
     * * @param elapsed The elapsed time between this call and the previous call of elapsed
     */
    public void update(long elapsed) {
        if (!isAlive) {
            // Now update the sprites animation and position
            player.update(elapsed);
            // Wait for 150ms before showing the screen
            deathTimer += elapsed;
            if (deathTimer >= 150) { 
                showDeathScreen = true;
            }
            return;
        }

        total += elapsed;

        // Make adjustments to the speed of the sprite due to gravity
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
        
        // Handle Animations properly
        if (moveRight) {
            player.setVelocityX(moveSpeed);
            if (!onGround) { if (player.getAnimation() != jump) player.setAnimation(jump); }
            else { if (player.getAnimation() != walk) player.setAnimation(walk); }
            player.setScale(2.0f, 2.0f); 
        } else if (moveLeft) {
            player.setVelocityX(-moveSpeed);
            if (!onGround) { if (player.getAnimation() != jumpL) player.setAnimation(jumpL); }
            else { if (player.getAnimation() != walkR) player.setAnimation(walkR); }
            player.setScale(-2.0f, 2.0f); 
        } else {
            player.setVelocityX(0);
            if (!onGround) { if (player.getAnimation() != jump) player.setAnimation(jump); }
            else { if (player.getAnimation() != landing) player.setAnimation(landing); }
            player.setScale(2.0f, 2.0f);
        }
        
        for (Sprite s: clouds) s.update(elapsed);
        
        // Now update the sprites animation and position
        player.update(elapsed);
       
        // Then check for any collisions that may have occurred
        handleScreenEdge(player, tmap, elapsed);
        checkTileCollision(player, tmap);
        
        if (jumpPressed && onGround) {
            player.setVelocityY(-0.20f);
            onGround = false;
            jumpPressed = false;
        }
    }
    
    /**
     * Checks and handles collisions with the edge of the screen. You should generally
     * use tile map collisions to prevent the player leaving the game area. This method
     * is only included as a temporary measure until you have properly developed your
     * tile maps.
     * * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     * @param elapsed	How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
    	// This method just checks if the sprite has gone off the bottom screen.
    	// Ideally you should use tile collision instead of this approach
    	
        if (s.getY() + 32.0f > tmap.getPixelHeight()) {
            if (isAlive) {
                isAlive = false;
                s.setVelocity(0, 0);
                death.start(); // Resets animation to frame 0
                s.setAnimation(death);
            }
        }
    }

    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * * @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) { 
        int key = e.getKeyCode();
        
        switch (key)
        {
            case KeyEvent.VK_UP: jumpPressed = true; break;
            case KeyEvent.VK_RIGHT: moveRight = true; break;
            case KeyEvent.VK_LEFT: moveLeft = true; break;
            case KeyEvent.VK_ESCAPE: stop(); System.exit(0); break;
            case KeyEvent.VK_B: debug = !debug; break; // Flip the debug state
            default: break;
        }
    }

    /** Use the sample code in the lecture notes to properly detect
     * a bounding box collision between sprites s1 and s2.
     * * @return	true if a collision may have occurred, false if it has not.
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2) {
        return false;   	
    }
    
    /**
     * Check and handles collisions with a tile map for the
     * given sprite 's'. Initial functionality is limited...
     * * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     */
    public void checkTileCollision(Sprite s, TileMap tmap) {
    	// Empty out our current set of collided tiles
        collidedTiles.clear();
        
        // Take a note of a sprite's current position
        float sx = s.getX(), sy = s.getY();
        float pSize = 32.0f; 
        float swidth = pSize * 0.8f; 
        float offsetX = (pSize - swidth) / 2;
        
        // Find out how wide and how tall a tile is
        float tileWidth = tmap.getTileWidth(), tileHeight = tmap.getTileHeight();
        
        // Divide the sprite's x coordinate by the width of a tile, to get
    	// the number of tiles across the x axis that the sprite is positioned at
        int xL = (int) ((sx + offsetX) / tileWidth);
        int xR = (int) ((sx + offsetX + swidth - 1) / tileWidth);
        int xMid = (int) ((sx + (pSize / 2)) / tileWidth); 
        
        // The same applies to the y coordinate
        int yB = (int) ((sy + pSize) / tileHeight); 
        
        // What tile character is at the bottom of the sprite s?
        Tile fL = tmap.getTile(xL, yB), fR = tmap.getTile(xR, yB), fMid = tmap.getTile(xMid, yB);
        
        onGround = false; 
        
        // If it's not empty space
        if (s.getVelocityY() >= 0) {
            if ((fL != null && fL.getCharacter() != '.') || (fR != null && fR.getCharacter() != '.') || (fMid != null && fMid.getCharacter() != '.')) {
                Tile t = (fMid != null && fMid.getCharacter() != '.') ? fMid : (fL != null && fL.getCharacter() != '.') ? fL : fR;
                
                // You should move the sprite to a position that is not colliding
                s.setY(t.getYC() - pSize); 
                s.setVelocityY(0); 
                onGround = true;
                collidedTiles.add(t);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Detect click on restart button when screen is active
        if (showDeathScreen && restartBtn.contains(e.getPoint())) {
            initialiseGame();
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public void keyReleased(KeyEvent e) { 
        int key = e.getKeyCode();
        
        switch (key)
        {
            case KeyEvent.VK_ESCAPE: stop(); break;
            case KeyEvent.VK_UP: jumpPressed = false; break;
            case KeyEvent.VK_RIGHT: moveRight = false; break;
            case KeyEvent.VK_LEFT: moveLeft = false; break;
            default: break;
        }
    }
}

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
    boolean levelCompleted = false; // Flag for level completion
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
    int currentLevel = 1; // Track current level

    // UI Buttons
    Rectangle restartBtn = new Rectangle(screenWidth/2 - 50, screenHeight/2 + 20, 100, 40);
    Rectangle nextBtn = new Rectangle(screenWidth/2 - 50, screenHeight/2 + 20, 100, 40);

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
        // NOTE: This method automatically loads images defined in the txt file!
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
        
        // Create a set of background sprites
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

        death = new Animation();
        death.loadAnimationFromSheet("images/Death.png", 1, 1, 150); 
        death.setLoop(false); 

        // Initialise the player
        player = new Sprite(landing);
        player.setScale(2.0f);
        
        // Load cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);
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
        levelCompleted = false;
        deathTimer = 0; 
        player.setPosition(500, 2700); 
        player.setVelocity(0, 0);
        player.setAnimation(landing);
        player.show();
    }
    
    /**
     * Draw the current state of the game.
     */
    public void draw(Graphics2D g) {    
        int xo = -(int)player.getX() + (screenWidth / 2);
        int yo = -(int)player.getY() + (screenHeight / 2);

        int bgWidth = 800; 
        drawLoopingLayer(g, skyLayer, xo, 0.1f, 0, bgWidth);
        drawLoopingLayer(g, mountainLayer, xo, 0.3f, 0, bgWidth);
        drawLoopingLayer(g, treesBackLayer, xo, 0.5f, 0, bgWidth);
        drawLoopingLayer(g, treesFrontLayer, xo, 0.7f, 0, bgWidth);
        drawLoopingLayer(g, groundLayer, xo, 0.9f, 0, bgWidth);
        
        for (Sprite s: clouds) {
            s.setOffsets((int)(xo * 0.5f), yo); 
            s.draw(g);
        }

        tmap.draw(g, xo, yo); 
        player.setOffsets(xo, yo);
        player.draw(g);
        
        String scoreMsg = String.format("Score: %d", total/100);
        g.setColor(Color.darkGray);
        g.drawString(scoreMsg, getWidth() - 100, 50);
        
        if (debug) {
            g.setColor(Color.red);
            player.drawBoundingBox(g);
            drawCollidedTiles(g, tmap, xo, yo);
        }

        if (showDeathScreen) drawDeathUI(g);
        if (levelCompleted) drawWinUI(g);
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
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, screenWidth, screenHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 60));
        g.drawString("YOU DIED", (screenWidth - g.getFontMetrics().stringWidth("YOU DIED")) / 2, screenHeight / 2 - 20);
        g.setColor(Color.RED);
        g.fill(restartBtn);
        g.setColor(Color.WHITE);
        g.draw(restartBtn);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Restart", restartBtn.x + 18, restartBtn.y + 26);
    }

    private void drawWinUI(Graphics2D g) {
        g.setColor(new Color(0, 100, 0, 180)); 
        g.fillRect(0, 0, screenWidth, screenHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("LEVEL COMPLETED!", (screenWidth - g.getFontMetrics().stringWidth("LEVEL COMPLETED!")) / 2, screenHeight / 2 - 20);
        g.setColor(Color.BLUE);
        g.fill(nextBtn);
        g.setColor(Color.WHITE);
        g.draw(nextBtn);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Next", nextBtn.x + 30, nextBtn.y + 26);
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
        if (!isAlive || levelCompleted) {
            player.update(elapsed);
            if (!isAlive && !levelCompleted) {
                deathTimer += elapsed;
                if (deathTimer >= 150) showDeathScreen = true;
            }
            return;
        }

        total += elapsed;
        player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
        
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
        player.update(elapsed);
        handleScreenEdge(player, tmap, elapsed);
        checkTileCollision(player, tmap);
        
        if (jumpPressed && onGround) {
            player.setVelocityY(-0.17f);
            onGround = false;
            jumpPressed = false;
        }
    }
    
    /**
     * Checks and handles collisions with the edge of the screen.
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
        if (s.getY() + 32.0f > tmap.getPixelHeight()) {
            if (isAlive) {
                isAlive = false;
                s.setVelocity(0, 0);
                death.start(); 
                s.setAnimation(death);
            }
        }
    }

    /**
     * Override of the keyPressed event.
     */
    public void keyPressed(KeyEvent e) { 
        int key = e.getKeyCode();
        switch (key)
        {
            case KeyEvent.VK_UP: jumpPressed = true; break;
            case KeyEvent.VK_RIGHT: moveRight = true; break;
            case KeyEvent.VK_LEFT: moveLeft = true; break;
            case KeyEvent.VK_ESCAPE: stop(); System.exit(0); break;
            case KeyEvent.VK_B: debug = !debug; break;
        }
    }

    public boolean boundingBoxCollision(Sprite s1, Sprite s2) { return false; }
    
    /**
     * Check and handles collisions with a tile map for the given sprite 's'.
     */
    public void checkTileCollision(Sprite s, TileMap tmap) {
        collidedTiles.clear();
        float sx = s.getX(), sy = s.getY();
        float pSize = 32.0f; 
        float swidth = pSize * 0.8f; 
        float offsetX = (pSize - swidth) / 2;
        float tileWidth = tmap.getTileWidth(), tileHeight = tmap.getTileHeight();
        int xL = (int) ((sx + offsetX) / tileWidth);
        int xR = (int) ((sx + offsetX + swidth - 1) / tileWidth);
        int xMid = (int) ((sx + (pSize / 2)) / tileWidth); 
        int yB = (int) ((sy + pSize) / tileHeight); 
        Tile fL = tmap.getTile(xL, yB), fR = tmap.getTile(xR, yB), fMid = tmap.getTile(xMid, yB);
        onGround = false; 
        if (s.getVelocityY() >= 0) {
            // Death tile check
            if ((fL != null && fL.getCharacter() == 's') || (fR != null && fR.getCharacter() == 's') || (fMid != null && fMid.getCharacter() == 's')) {
                isAlive = false; s.setVelocity(0, 0); death.start(); s.setAnimation(death); return;
            }
            // Win tile check
            if ((fL != null && fL.getCharacter() == 'o') || (fR != null && fR.getCharacter() == 'o') || (fMid != null && fMid.getCharacter() == 'o')) {
                levelCompleted = true; s.setVelocity(0, 0); return;
            }
            // Ground check
            if ((fL != null && fL.getCharacter() != '.') || (fR != null && fR.getCharacter() != '.') || (fMid != null && fMid.getCharacter() != '.')) {
                Tile t = (fMid != null && fMid.getCharacter() != '.') ? fMid : (fL != null && fL.getCharacter() != '.') ? fL : fR;
                s.setY(t.getYC() - pSize); s.setVelocityY(0); onGround = true; collidedTiles.add(t);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (showDeathScreen && restartBtn.contains(e.getPoint())) initialiseGame();
        if (levelCompleted && nextBtn.contains(e.getPoint())) {
            currentLevel++;
            tmap.loadMap("maps", "level" + currentLevel + ".txt");
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
        }
    }
}

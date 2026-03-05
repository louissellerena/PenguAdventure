import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import game2D.*;

//Game demonstrates how we can override the GameCore class
//to create our own 'game'. We usually need to implement at
//least 'draw' and 'update' (not including any local event handling)
//to begin the process. You should also add code to the 'init'
//method that will initialise event handlers etc. 

//Student ID: 3359480
@SuppressWarnings("serial")
public class Game extends GameCore implements MouseListener
{
	// Useful game constants
    static int screenWidth = 800; 
    static int screenHeight = 600;
    
    // Game constants
    float gravity = 0.0001f;
    float moveSpeed = 0.05f;
    
    //Game state flags
    boolean moveRight = false, moveLeft = false, onGround = false, jumpPressed = false, debug = false;
    boolean isAlive = true;
    boolean showDeathScreen = false; 
    //Game resources
    Animation landing, walk, walkR, jump, jumpL, death;
    
    Sprite player = null;
    ArrayList<Sprite> clouds = new ArrayList<Sprite>();
    ArrayList<Tile> collidedTiles = new ArrayList<Tile>();
    
    TileMap tmap = new TileMap(); // Our tile map, note that we load it in init()

    long total; 				// The score will be the total time elapsed since a crash
    
    // UI Button - Centered on screen
    Rectangle restartBtn = new Rectangle(screenWidth/2 - 50, screenHeight/2 + 20, 100, 40);

    public static void main(String[] args) {
        Game gct = new Game();
        gct.init();
        gct.run(false, screenWidth, screenHeight);
    }

    public void init()
    {          
        tmap.loadMap("maps", "level1.txt");
        setSize(screenWidth, screenHeight);
        setVisible(true);
        
        addMouseListener(this); 
        
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

        player = new Sprite(landing);
        player.setScale(2.0f);
        
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);
        for (int c=0; c<6; c++) {
            Sprite s = new Sprite(ca);
            s.setX((int)(Math.random() * tmap.getPixelWidth()));
            s.setY(50 + (int)(Math.random() * 200.0f));
            s.setVelocityX(-0.01f);
            s.show();
            clouds.add(s);
        }
        initialiseGame();
    }

    public void initialiseGame() {
    	total = 0;
        isAlive = true;
        showDeathScreen = false; 
        player.setPosition(200, 200);
        player.setVelocity(0, 0);
        player.setAnimation(landing);
        player.show();
    }
    
    public void draw(Graphics2D g) {       
        int xo = -(int)player.getX() + (screenWidth / 2);
        int yo = -(int)player.getY() + (screenHeight / 2);

        // Sky background
        g.setColor(new Color(135, 206, 235)); 
        g.fillRect(0, 0, getWidth(), getHeight());
        
        for (Sprite s: clouds) {
            s.setOffsets((int)(xo * 0.5f), yo); 
            s.draw(g);
        }

        tmap.draw(g, xo, yo); 
        player.setOffsets(xo, yo);
        player.draw(g);
        
        String msg = String.format("Score: %d", total/100);
        g.setColor(Color.darkGray);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(msg, getWidth() - 100, 30);
        
        if (debug) {
            g.setColor(Color.red);
            player.drawBoundingBox(g);
            drawCollidedTiles(g, tmap, xo, yo);
        }

        // ui drawn last to be on top
        if (showDeathScreen) {
            drawDeathUI(g);
        }
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

    public void update(long elapsed) {
        if (!isAlive) {
            player.update(elapsed);
            // If the death animation has played through once
            if (death.hasLooped()) { 
                showDeathScreen = true;
            }
            return;
        }
        
        total += elapsed; //increment score while alive

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
        player.update(elapsed);
        handleScreenEdge(player, tmap, elapsed);
        checkTileCollision(player, tmap);
        
        if (jumpPressed && onGround) {
            player.setVelocityY(-0.25f);
            onGround = false;
            jumpPressed = false;
        }
    }
    
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
        if (s.getY() + 32.0f > tmap.getPixelHeight()) {
            if (isAlive) {
                isAlive = false;
                s.setVelocity(0, 0);
                death.start(); // Resets animation to frame 0
                s.setAnimation(death);
            }
        }
    }

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
            if ((fL != null && fL.getCharacter() != '.') || (fR != null && fR.getCharacter() != '.') || (fMid != null && fMid.getCharacter() != '.')) {
                Tile t = (fMid != null && fMid.getCharacter() != '.') ? fMid : (fL != null && fL.getCharacter() != '.') ? fL : fR;
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

    public void keyPressed(KeyEvent e) { 
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) jumpPressed = true;
        if (key == KeyEvent.VK_RIGHT) moveRight = true;
        if (key == KeyEvent.VK_LEFT) moveLeft = true;
        if (key == KeyEvent.VK_ESCAPE) { stop(); System.exit(0); }
        if (key == KeyEvent.VK_B) debug = !debug;
    }

    public void keyReleased(KeyEvent e) { 
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) jumpPressed = false;
        if (key == KeyEvent.VK_RIGHT) moveRight = false;
        if (key == KeyEvent.VK_LEFT) moveLeft = false;
    }

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
        if (collidedTiles.size() > 0) {   
            g.setColor(Color.blue);
            for (Tile t : collidedTiles)
                g.drawRect(t.getXC() + xOffset, t.getYC() + yOffset, map.getTileWidth(), map.getTileHeight());
        }
    }
}

package org.skarph.shimenji;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.json.JSONObject;
import org.skarph.shimenji.ShimenjiState.DragState;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

//TODO: fix spaghetti code so it isnt served al dente
public class App {
	private JFrame frame;
	private Image[][] spriteMap;
	private ShimenjiState shimenjiState;
	private static boolean DEBUG = false;
	private static final int TIMESTEP = 10; //timestep, in milliseconds
	
	public static String Sprite_Path = System.getProperty("user.dir") + File.separator + "sprites.png";
	public static String Animation_Path = System.getProperty("user.dir") + File.separator + "animation.json";
	int mX=0; //mouse x, used in dragging
    int mY=0; //mouse y, used in dragging
	
    public static void main(String[] args) throws InvalidClassException {
		Image[][] spriteMap2 = null;
		ShimenjiState state2 = null;
		if(args.length>=3)
			DEBUG = true;
		if(args.length==2) {
			Sprite_Path=args[0];
			Animation_Path=args[1];
		}
		try {
			System.out.println(new String(Files.readAllBytes(Paths.get(Animation_Path))));
			JSONObject jsono = new JSONObject(new String(Files.readAllBytes(Paths.get(Animation_Path))));
			state2 = new ShimenjiState(jsono,Toolkit.getDefaultToolkit().getScreenSize().getWidth(), Toolkit.getDefaultToolkit().getScreenSize().getHeight());
			spriteMap2 = loadSprites(new File(Sprite_Path),state2.spriteW,state2.spriteH);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}
		
		//finalize
		final ShimenjiState state = state2;
		final Image[][] spriteMap = spriteMap2;
		
		EventQueue.invokeLater(new Runnable() {
			
			public void run() {
				App window = null;
				try {
					window = new App(state, spriteMap);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				final App windowF = window;
				new Timer(TIMESTEP, new ActionListener() {
				  @Override
				  public void actionPerformed(ActionEvent evt) {
					  state.update(0.001*TIMESTEP);
					  if(state.dragState != DragState.DRAGGING)
						  windowF.frame.setLocation((int) state.pos[0],(int) state.pos[1]);
					  windowF.frame.getContentPane().repaint();
				  }
				}).start();
			}
			
		});
		
	}
	
	public App(ShimenjiState s, Image[][] m) {
		shimenjiState = s;
		spriteMap = m;
		initialize();
	}
	
	private void initialize() {
		frame = new JFrame();
		
		frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0,0,0,1));
        frame.setResizable(false);
        frame.setAutoRequestFocus(false);

		frame.setBounds(0, 0,  DEBUG?300:shimenjiState.spriteW, DEBUG?300:shimenjiState.spriteH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel() {
			@Override
            protected void paintComponent(Graphics g) {
                if (g instanceof Graphics2D) {
                    Graphics2D g2d = (Graphics2D)g;
                    g2d.setPaint(new Color(0f,0f,0f, DEBUG?0.5f:0.0f));
                    //g2d.clearRect(0,0, getWidth(), getHeight());
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    drawSprite(g2d);
                }
            }
        };
		
        //dragging & mouse handling code
        panel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				frame.setLocation(e.getXOnScreen()-mX, e.getYOnScreen()-mY);
			}
		});
		
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mX=e.getX();
				mY=e.getY();
				shimenjiState.pickUp();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				shimenjiState.drop(e.getXOnScreen(), e.getYOnScreen(), (e.getXOnScreen()-mX)/(0.001*TIMESTEP));
			}
		});
		
        panel.setOpaque(false);
		frame.setContentPane(panel);
	}
	
	
	//imports sprites from a file. sprites are assumed w by h pixels
	private static Image[][] loadSprites(File f, int w, int h) throws IOException{
		BufferedImage sheet = ImageIO.read(f);
		
		int cX = sheet.getWidth()/w; //sub-animation count
		int cY = sheet.getHeight()/h; //state count
		Image[][] spriteMap = new Image[cY][cX];
		for(int y=0; y < cY; y++) {
			for(int x=0; x < cX; x++) {
				spriteMap[y][x] = sheet.getSubimage(x*w,y*h,w,h);
			}
		}
		return spriteMap;
	}
	
	private void drawSprite(Graphics2D g2d) {
		int row = shimenjiState.currentAnimation.animation;
		int frame =  (int) ( (shimenjiState.t / shimenjiState.currentAnimation.rate) % spriteMap[shimenjiState.currentAnimation.animation].length);
		g2d.drawImage(spriteMap[row][frame], 0, 0, null);
        if(DEBUG)
        	debugPrint(g2d, 0, 100);
	}
	
	void debugPrint(Graphics2D g, int x, int y) {
		g.setPaint(new Color(1f,1f,1f,1f));
        g.setFont( new Font("Serif", Font.PLAIN, 20));
		String debugString = String.format("Anim/Frame: %d / %d\nPos: %f , %f\nTime: %f\nAniName: %s\nx = %s\ny = %s",
				shimenjiState.currentAnimation.animation, (int) ( (shimenjiState.t / shimenjiState.currentAnimation.rate) % spriteMap[shimenjiState.currentAnimation.animation].length),
        		shimenjiState.pos[0], shimenjiState.pos[1],
        		shimenjiState.t, shimenjiState.currentAnimation.name,
        		shimenjiState.currentAnimation.xFormula.getFunctionExpressionString(),
        		shimenjiState.currentAnimation.yFormula.getFunctionExpressionString()
        		);
	    for (String line : debugString.split("\n"))
	        g.drawString(line, x, y += g.getFontMetrics().getHeight());
	}
}

package com.lostvault;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

/** Desktop entry point. Uses a single Java2D canvas and no external runtime assets. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The Lost Vault needs a graphical desktop. Run ./gradlew runGame from a desktop session.");
            return;
        }
        try {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("The Lost Vault | A Treasure-Hunt Adventure");
                GamePanel panel = new GamePanel(); frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){panel.disposeGame();frame.dispose();}});
                frame.setContentPane(panel);frame.setMinimumSize(new Dimension(900,600));frame.setSize(1280,720);frame.setLocationRelativeTo(null);frame.setVisible(true);panel.requestFocusInWindow();panel.start();
            });
        } catch (AWTError | HeadlessException ex) {
            System.err.println("Unable to initialize the desktop window. Run in a graphical session or under Xvfb. Details: " + ex.getMessage());
        }
    }

    @SuppressWarnings("serial")
    static final class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
        private final GameModel game = new GameModel(); private final Timer timer = new Timer(16,this); private final boolean[] keys=new boolean[512];
        private long lastNanos=System.nanoTime();private int menuIndex=0;private int mouseX,mouseY;private boolean disposed=false;private double walkPhase=0;
        GamePanel(){setFocusable(true);addKeyListener(this);addMouseListener(this);addMouseMotionListener(this);setBackground(new Color(8,20,28));setDoubleBuffered(true);}
        void start(){timer.start();}
        void disposeGame(){disposed=true;timer.stop();Window window=SwingUtilities.getWindowAncestor(this);if(window!=null)window.dispose();}
        @Override public void actionPerformed(ActionEvent e){long now=System.nanoTime();double dt=Math.min(.05,(now-lastNanos)/1_000_000_000.0);lastNanos=now;
            if(game.state()==GameModel.State.PLAYING&&!game.dialogueOpen()&&!game.journalOpen()){double dx=(down(KeyEvent.VK_D,KeyEvent.VK_RIGHT)?1:0)-(down(KeyEvent.VK_A,KeyEvent.VK_LEFT)?1:0);double dy=(down(KeyEvent.VK_S,KeyEvent.VK_DOWN)?1:0)-(down(KeyEvent.VK_W,KeyEvent.VK_UP)?1:0);if(dx!=0||dy!=0)walkPhase+=dt*10;game.update(dt,dx,dy,false);}
            repaint();}
        private boolean down(int... codes){for(int c:codes)if(c<keys.length&&keys[c])return true;return false;}
        @Override protected void paintComponent(Graphics g0){super.paintComponent(g0);Graphics2D g=(Graphics2D)g0.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();if(game.state()==GameModel.State.MENU)drawMenu(g,w,h);else if(game.state()==GameModel.State.HOW_TO_PLAY)drawHowTo(g,w,h);else if(game.state()==GameModel.State.VICTORY)drawVictory(g,w,h);else if(game.state()==GameModel.State.TRANSITION)drawTransition(g,w,h);else if(game.level()!=null){Graphics2D world=(Graphics2D)g.create();drawWorld(world,w,h);world.dispose();if(game.state()==GameModel.State.PAUSED)drawPause(g,w,h);if(game.dialogueOpen())drawDialogue(g,w,h);if(game.journalOpen())drawJournal(g,w,h);}g.dispose();}
        private void drawMenu(Graphics2D g,int w,int h){gradient(g,w,h,new Color(8,29,43),new Color(16,68,71));drawIslandSilhouette(g,w,h);g.setColor(new Color(245,190,91));g.setFont(new Font("Serif",Font.BOLD,Math.max(40,w/13)));center(g,"THE LOST VAULT",w/2,h/3);g.setColor(new Color(221,236,223));g.setFont(new Font("SansSerif",Font.PLAIN,18));center(g,"A TREASURE-HUNT ADVENTURE",w/2,h/3+38);
            String[] opts={"START NEW EXPEDITION","HOW TO PLAY","QUIT"};int y=h/2+42;for(int i=0;i<opts.length;i++){boolean sel=i==menuIndex;g.setColor(sel?new Color(246,177,72,235):new Color(5,23,31,180));round(g,w/2-175,y-29,350,48,14,true);g.setColor(sel?new Color(29,42,38):new Color(225,235,220));g.setFont(new Font("SansSerif",sel?Font.BOLD:Font.PLAIN,16));center(g,opts[i],w/2,y+1);y+=64;}g.setColor(new Color(180,208,195));g.setFont(new Font("SansSerif",Font.PLAIN,12));center(g,"WASD / ARROWS TO MOVE   •   SPACE TO INTERACT   •   ESC TO PAUSE",w/2,h-28);}
        private void drawIslandSilhouette(Graphics2D g,int w,int h){g.setColor(new Color(16,91,83));g.fill(new Ellipse2D.Double(w*.08,h*.58,w*.84,h*.55));g.setColor(new Color(31,115,83));g.fill(new Ellipse2D.Double(w*.18,h*.63,w*.64,h*.4));Random r=new Random(7);for(int i=0;i<24;i++){int x=(int)(r.nextDouble()*w),y=(int)(h*.6+r.nextDouble()*h*.35);g.setColor(new Color(9,68,59,180));g.fillOval(x,y,18,30);}}
        private void drawWorld(Graphics2D g,int w,int h){double scale=Math.min(w/1280.0,h/720.0);double ox=(w-1280*scale)/2,oy=(h-720*scale)/2;g.translate(ox,oy);g.scale(scale,scale);w=1280;h=720;GameModel.Level l=game.level();double camX=Math.max(0,Math.min(GameModel.WORLD_W-1280,game.playerX()+14-640)),camY=Math.max(0,Math.min(GameModel.WORLD_H-720,game.playerY()+14-360));
            Color ground=l.number==1?new Color(210,183,123):l.number==2?new Color(48,103,69):new Color(91,118,92);g.setColor(ground);g.fillRect(0,0,1280,720);g.translate(-camX,-camY);
            // Grid-based exploration map: orthogonal trails, intersections, loops, and optional branches.
            // The world remains free-roaming, but the trail network gives players readable choices.
            Color trail = l.number==1?new Color(231,207,151):l.number==2?new Color(81,121,75):new Color(135,151,115);
            Color trailEdge = l.number==1?new Color(184,151,94,100):l.number==2?new Color(24,68,43,125):new Color(64,88,65,120);
            g.setStroke(new BasicStroke(98,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));
            g.setColor(trailEdge);
            int[][][] routes = l.number==1 ? new int[][][] {
                {{100,160},{400,160},{400,400},{640,400},{640,640},{880,640},{880,400},{1120,400},{1120,640},{1360,640},{1360,1040},{1660,1040}},
                {{400,400},{160,400},{160,640},{400,640},{400,880},{640,880}},
                {{880,640},{1120,880},{1360,880},{1360,1040}},
                {{640,400},{880,160},{1120,160},{1360,400}}
            } : l.number==2 ? new int[][][] {
                {{100,160},{400,160},{400,400},{640,400},{640,640},{880,640},{880,400},{1120,400},{1120,640},{1360,640},{1360,880},{1600,880}},
                {{400,400},{160,400},{160,640},{400,640},{400,880},{640,880},{640,1040}},
                {{640,400},{640,160},{880,160},{1120,160},{1360,160},{1360,400}},
                {{880,640},{1120,880},{1360,880},{1600,1040}}
            } : new int[][][] {
                {{100,160},{400,160},{400,400},{640,400},{640,640},{880,640},{880,400},{1120,400},{1360,400},{1360,640},{1360,880},{1600,880}},
                {{400,400},{160,400},{160,640},{400,640},{400,880},{640,880},{880,880}},
                {{640,400},{640,160},{880,160},{1120,160}},
                {{880,640},{1120,640},{1120,880},{1360,880},{1600,1040}}
            };
            for(int[][] route:routes){for(int i=1;i<route.length;i++)g.drawLine(route[i-1][0],route[i-1][1],route[i][0],route[i][1]);}
            g.setColor(trail);g.setStroke(new BasicStroke(82,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));
            for(int[][] route:routes){for(int i=1;i<route.length;i++)g.drawLine(route[i-1][0],route[i-1][1],route[i][0],route[i][1]);}
            g.setStroke(new BasicStroke(1));
            // Subtle cartographer's grid and junction stones make the branching layout legible.
            g.setColor(new Color(18,35,25,24));g.setStroke(new BasicStroke(1));
            for(int gx=160;gx<GameModel.WORLD_W;gx+=80)g.drawLine(gx,0,gx,GameModel.WORLD_H);
            for(int gy=80;gy<GameModel.WORLD_H;gy+=80)g.drawLine(0,gy,GameModel.WORLD_W,gy);
            g.setColor(new Color(246,224,169,105));
            for(int gx=160;gx<=1600;gx+=240)for(int gy=160;gy<=1040;gy+=240)g.fillOval(gx-3,gy-3,6,6);
            for(int i=0;i<55;i++){int x=(i*193+77)%GameModel.WORLD_W,y=(i*127+49)%GameModel.WORLD_H;g.setColor(new Color(255,255,255,18));g.fillOval(x,y,3,3);}
            for(GameModel.Item it:l.items)if(it.active)drawItem(g,it,l.number);
            // Player: animated primitive body with hierarchical limb pivots.
            double px=game.playerX(),py=game.playerY(),bob=(Math.sin(walkPhase)*2);
            g.setColor(new Color(0,0,0,55));g.fillOval((int)px-3,(int)py+20,34,12);
            g.setColor(new Color(32,52,64));g.fillRoundRect((int)px+5,(int)(py+4+bob),19,23,8,8);g.setColor(new Color(230,177,125));g.fillOval((int)px+6,(int)(py-2+bob),17,17);g.setColor(new Color(30,40,42));g.fillArc((int)px+5,(int)(py-5+bob),19,13,0,180);
            double arm=Math.sin(walkPhase)*4;g.setColor(new Color(194,135,91));g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(px+8,py+12,px+3,py+23+arm));g.draw(new Line2D.Double(px+20,py+12,px+25,py+23-arm));g.setColor(new Color(46,57,62));g.setStroke(new BasicStroke(6,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(px+10,py+23,px+8+Math.sin(walkPhase)*3,py+31));g.draw(new Line2D.Double(px+19,py+23,px+21-Math.sin(walkPhase)*3,py+31));g.setStroke(new BasicStroke(1));
            g.translate(camX,camY);drawHud(g,1280,720);}
        private void drawItem(Graphics2D g,GameModel.Item i,int level){int x=(int)i.bounds.x(),y=(int)i.bounds.y(),w=(int)i.bounds.w(),h=(int)i.bounds.h();switch(i.kind){case GEM->{g.setColor(new Color(0,0,0,50));g.fillOval(x-2,y+h-1,w+4,8);Polygon p=new Polygon(new int[]{x+w/2,x+w,x+w/2,x},new int[]{y,y+h/3,y+h,y+h/3},4);g.setColor(new Color(58,222,222));g.fillPolygon(p);g.setColor(new Color(210,255,249));g.drawLine(x+w/2,y+3,x+w/2,y+h-3);g.setColor(new Color(255,255,255,110));g.drawLine(x+w/2,y+3,x+w-3,y+h/3);}
            case CLUE->{g.setColor(new Color(84,48,31));g.fillRoundRect(x,y,w,h,5,5);g.setColor(new Color(244,223,169));g.fillRect(x+4,y+3,w-8,h-6);g.setColor(new Color(139,91,51));for(int k=0;k<3;k++)g.drawLine(x+8,y+9+k*6,x+w-8,y+9+k*6);}
            case HAZARD->{g.setColor(new Color(53,45,34,100));g.fillRoundRect(x,y,w,h,20,20);g.setColor(new Color(194,104,53));g.setStroke(new BasicStroke(3));for(int k=0;k<3;k++){int tx=x+12+k*(w-24)/2;g.drawLine(tx,y+h-8,tx+8,y+8);g.drawLine(tx+8,y+8,tx+16,y+h-8);}g.setStroke(new BasicStroke(1));}
            case CHECKPOINT->{g.setColor(new Color(243,196,78,55));g.fillOval(x-9,y-8,w+18,h+18);g.setColor(new Color(245,202,83));g.fillOval(x+5,y+5,w-10,h-10);g.setColor(new Color(255,245,187));g.drawOval(x+9,y+9,w-18,h-18);}
            case GATE->{g.setColor(new Color(47,54,49));g.fillRoundRect(x,y,w,h,6,6);g.setColor(new Color(183,151,89));for(int k=0;k<4;k++)g.fillRect(x+7+k*11,y+4,5,h-8);g.setColor(new Color(241,210,128));g.drawRect(x+3,y+3,w-6,h-6);}
            case EXIT->{g.setColor(new Color(255,226,143,80));g.fillOval(x-14,y-10,w+28,h+20);g.setColor(new Color(64,87,58));g.fillRoundRect(x,y,w,h,12,12);g.setColor(new Color(238,215,142));g.drawRoundRect(x+7,y+7,w-14,h-14,8,8);g.setFont(new Font("SansSerif",Font.BOLD,10));g.drawString("PATH",x+15,y+h/2);}
            case VAULT->{g.setColor(new Color(0,0,0,55));g.fillOval(x-5,y+h-3,w+10,14);g.setColor(new Color(103,88,69));g.fillRoundRect(x,y,w,h,10,10);g.setColor(new Color(193,165,103));g.fillRoundRect(x+9,y+10,w-18,h-18,8,8);g.setColor(new Color(68,62,48));g.fillRect(x+w/2-8,y+h/2-4,16,22);g.setColor(new Color(255,222,124));g.fillOval(x+w/2-4,y+h/2+2,8,8);g.setColor(new Color(242,220,157));g.setFont(new Font("Serif",Font.BOLD,12));g.drawString("VAULT",x+22,y-7);}
            case TREASURE->{g.setColor(new Color(255,213,78,80));g.fillOval(x-10,y-10,w+20,h+20);g.setColor(new Color(139,76,37));g.fillRoundRect(x,y+8,w,h-8,6,6);g.setColor(new Color(235,176,58));g.fillRoundRect(x,y,w,h/2+8,6,6);g.setColor(new Color(255,231,136));g.fillRect(x+4,y+h/2,w-8,4);}
            case BOAT->{g.setColor(new Color(0,0,0,45));g.fillOval(x-5,y+h-2,w+10,12);Polygon boat=new Polygon(new int[]{x,x+12,x+w-12,x+w},new int[]{y+18,y+h,y+h,y+18},4);g.setColor(new Color(116,67,40));g.fillPolygon(boat);g.setColor(new Color(229,216,174));g.fillRect(x+18,y+12,w-36,7);g.setColor(new Color(255,255,255,140));g.fillPolygon(new int[]{x+24,x+52,x+24},new int[]{y+11,y+11,y-20},3);}
            case TREE->{g.setColor(new Color(104,73,44));g.fillRoundRect(x+20,y+30,15,40,5,5);g.setColor(new Color(23,85,57));g.fillOval(x+1,y+8,48,42);g.setColor(new Color(42,112,67));g.fillOval(x+12,y,36,39);g.setColor(new Color(76,143,76));g.fillOval(x+18,y+7,22,23);}
            case ROCK->{g.setColor(new Color(87,99,88));g.fillRoundRect(x,y,w,h,12,12);g.setColor(new Color(135,143,123));g.fillOval(x+7,y+4,w/2,h/2);}
            case RUIN->{g.setColor(new Color(112,111,87));g.fillRect(x,y,w,h);}
            case NPC->{g.setColor(new Color(0,0,0,55));g.fillOval(x-3,y+h-2,w+6,8);g.setColor(new Color(222,171,110));g.fillOval(x+7,y+2,w-14,17);g.setColor(new Color(65,42,31));g.fillArc(x+5,y-2,w-10,15,0,180);g.setColor(i.id.contains("Keeper")||i.id.equals("L3-NPC")?new Color(161,125,73):new Color(45,93,111));g.fillRoundRect(x+4,y+18,w-8,h-20,8,8);g.setColor(new Color(255,231,153));g.drawOval(x-4,y-7,w+8,h+8);}
            case INSCRIPTION->{g.setColor(new Color(95,91,70));g.fillRoundRect(x,y,w,h,5,5);g.setColor(new Color(206,190,143));g.fillRoundRect(x+4,y+3,w-8,h-6,4,4);g.setColor(new Color(94,84,57));for(int k=0;k<3;k++)g.drawLine(x+9,y+9+k*7,x+w-9,y+9+k*7);g.setColor(new Color(255,222,117,90));g.drawOval(x-5,y-5,w+10,h+10);}}
        }
        private void drawHud(Graphics2D g,int w,int h){g.setColor(new Color(5,22,28,220));round(g,18,16,440,78,16,true);g.setColor(new Color(242,205,119));g.setFont(new Font("SansSerif",Font.BOLD,15));g.drawString("✦  THE LOST VAULT",34,39);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,13));g.drawString("LIVES  "+"♥ ".repeat(Math.max(0,game.lives())),34,62);g.drawString("GEMS  "+game.levelGems()+" / "+game.level().gemRequirement,165,62);g.drawString("CLUES  "+game.collectedClues().size()+" / "+game.level().clueIds.size(),295,62);
            g.setColor(new Color(4,21,25,185));round(g,w-330,16,312,48,13,true);g.setColor(new Color(240,223,180));g.setFont(new Font("SansSerif",Font.BOLD,14));g.drawString("LEVEL "+game.levelNumber()+"  •  "+game.level().name, w-315,45);
            if(game.messageTimer()>0&&!game.message().isBlank()){g.setFont(new Font("SansSerif",Font.BOLD,14));FontMetrics fm=g.getFontMetrics();int mw=Math.min(700,fm.stringWidth(game.message())+42);g.setColor(new Color(4,20,24,220));round(g,(w-mw)/2,h-72,mw,44,12,true);g.setColor(new Color(255,235,181));center(g,game.message(),w/2,h-45);}
            if(game.isNearInteractable()){g.setColor(new Color(4,20,24,220));round(g,w/2-165,h-123,330,36,10,true);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,13));center(g,"PRESS SPACE / ENTER TO TALK, READ OR INTERACT",w/2,h-100);}
            g.setColor(new Color(255,255,255,140));g.setFont(new Font("SansSerif",Font.PLAIN,11));g.drawString("WASD / ARROWS MOVE   SPACE INTERACT   J JOURNAL   ESC PAUSE",24,h-16);
        }
        private void drawDialogue(Graphics2D g,int w,int h){
            g.setColor(new Color(2,12,16,190));g.fillRect(0,0,w,h);g.setColor(new Color(22,39,38,245));round(g,110,h-245,w-220,190,22,true);g.setColor(new Color(242,190,88));g.setStroke(new BasicStroke(2));round(g,110,h-245,w-220,190,22,false);g.setStroke(new BasicStroke(1));g.setFont(new Font("Serif",Font.BOLD,24));g.drawString(game.dialogueTitle(),140,h-204);g.setColor(new Color(239,236,214));g.setFont(new Font("SansSerif",Font.PLAIN,16));drawWrapped(g,game.dialogueText(),140,h-170,w-280,25);g.setColor(new Color(247,199,105));g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString("SPACE / ENTER — CONTINUE",140,h-77);
        }
        private void drawJournal(Graphics2D g,int w,int h){g.setColor(new Color(2,12,16,200));g.fillRect(0,0,w,h);g.setColor(new Color(35,43,35,248));round(g,150,65,w-300,h-130,20,true);g.setColor(new Color(236,193,101));g.setFont(new Font("Serif",Font.BOLD,29));g.drawString("EXPEDITION JOURNAL",185,110);g.setColor(new Color(224,231,211));g.setFont(new Font("SansSerif",Font.PLAIN,15));int y=150;g.drawString("Clues carried: "+(game.collectedClues().isEmpty()?"none yet":String.join(", ",game.collectedClues())),185,y);y+=34;g.drawString("Gems recovered this level: "+game.levelGems()+" / "+game.level().gemRequirement,185,y);y+=40;for(String entry:game.journalEntries()){for(String line:wrap(entry,Math.max(35,(w-390)/8))){if(y>h-105)break;g.drawString("• "+line,190,y);y+=23;}y+=8;if(y>h-105)break;}g.setColor(new Color(247,199,105));g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString("J / ESC — CLOSE JOURNAL",185,h-83);}
        private void drawWrapped(Graphics2D g,String text,int x,int y,int maxWidth,int lineHeight){FontMetrics fm=g.getFontMetrics();StringBuilder line=new StringBuilder();for(String word:text.split("\\s+")){String candidate=line.length()==0?word:line+" "+word;if(fm.stringWidth(candidate)>maxWidth&&line.length()>0){g.drawString(line.toString(),x,y);y+=lineHeight;line=new StringBuilder(word);}else line=new StringBuilder(candidate);}if(line.length()>0)g.drawString(line.toString(),x,y);}
        private java.util.List<String> wrap(String text,int maxChars){java.util.List<String> lines=new java.util.ArrayList<>();StringBuilder line=new StringBuilder();for(String word:text.split("\\s+")){if(line.length()+word.length()+1>maxChars&&line.length()>0){lines.add(line.toString());line.setLength(0);}if(line.length()>0)line.append(' ');line.append(word);}if(line.length()>0)lines.add(line.toString());return lines;}
        private void drawPause(Graphics2D g,int w,int h){g.setColor(new Color(3,14,19,190));g.fillRect(0,0,w,h);panelTitle(g,w,h,"EXPEDITION PAUSED");String[] opts={"RESUME","RESTART LEVEL","HOW TO PLAY","MAIN MENU","QUIT"};int y=h/2-12;for(int i=0;i<opts.length;i++){g.setColor(i==menuIndex?new Color(238,178,75):new Color(226,232,219));g.setFont(new Font("SansSerif",i==menuIndex?Font.BOLD:Font.PLAIN,17));center(g,(i==menuIndex?"›  ":"   ")+opts[i],w/2,y);y+=43;}}
        private void drawHowTo(Graphics2D g,int w,int h){gradient(g,w,h,new Color(14,39,46),new Color(32,73,61));panelTitle(g,w,h,"HOW TO PLAY");String[] lines={"MOVE     W A S D or Arrow Keys — move in eight directions","INTERACT     Space / Enter — open the Vault, collect treasure, use rescue boat","EXPLORE     Collect the required gems and clue scrolls in each level","HAZARDS     Quicksand, spikes and coral cost a life; you respawn at checkpoints","CHECKPOINTS     Touch a golden marker to secure your respawn location","LEVEL 1     3 gems + Clue A + the inland path","LEVEL 2     5 gems + Clues B and C + unlock the ancient gate","LEVEL 3     4 gems + Clues A, B and C + Vault + treasure + rescue boat","PAUSE     Escape pauses the simulation. Choose Restart Level or Main Menu."};g.setFont(new Font("SansSerif",Font.PLAIN,Math.max(14,w/90)));int y=h/2-105;for(String s:lines){g.setColor(new Color(231,239,222));g.drawString(s,Math.max(35,w/2-360),y);y+=34;}g.setColor(new Color(242,197,105));g.setFont(new Font("SansSerif",Font.BOLD,16));center(g,"ENTER / SPACE / ESC — BACK",w/2,h-55);}
        private void drawTransition(Graphics2D g,int w,int h){gradient(g,w,h,new Color(12,35,42),new Color(27,91,70));g.setColor(new Color(247,194,91));g.setFont(new Font("Serif",Font.BOLD,Math.max(30,w/24)));center(g,game.transitionText(),w/2,h/2-35);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,18));center(g,"The trail leads deeper into the island...",w/2,h/2+12);center(g,"PRESS ENTER / SPACE TO CONTINUE",w/2,h/2+78);}
        private void drawVictory(Graphics2D g,int w,int h){gradient(g,w,h,new Color(8,32,44),new Color(17,101,81));drawIslandSilhouette(g,w,h);g.setColor(new Color(255,216,106,55));g.fillOval(w/2-130,h/3-80,260,260);g.setColor(new Color(250,207,103));g.setFont(new Font("Serif",Font.BOLD,Math.max(36,w/18)));center(g,"TREASURE RECOVERED!",w/2,h/3);g.setColor(new Color(237,244,224));g.setFont(new Font("SansSerif",Font.PLAIN,18));center(g,"You escaped the island with the Lost Vault's secrets.",w/2,h/3+45);g.setColor(new Color(247,197,91));round(g,w/2-160,(int)(h*.66),320,52,14,true);g.setColor(new Color(35,45,34));g.setFont(new Font("SansSerif",Font.BOLD,15));center(g,"RETURN TO MAIN MENU",w/2,(int)(h*.66+32));}
        private void panelTitle(Graphics2D g,int w,int h,String title){g.setColor(new Color(245,199,104));g.setFont(new Font("Serif",Font.BOLD,Math.max(30,w/28)));center(g,title,w/2,h/4);}
        private void gradient(Graphics2D g,int w,int h,Color a,Color b){g.setPaint(new GradientPaint(0,0,a,0,h,b));g.fillRect(0,0,w,h);}
        private void round(Graphics2D g,int x,int y,int w,int h,int r,boolean fill){if(fill)g.fillRoundRect(x,y,w,h,r,r);else g.drawRoundRect(x,y,w,h,r,r);}
        private void center(Graphics2D g,String text,int x,int y){FontMetrics fm=g.getFontMetrics();g.drawString(text,x-fm.stringWidth(text)/2,y);}
        private void activateMenu(){switch(game.state()){case MENU->{if(menuIndex==0){game.startNewRun();menuIndex=0;}else if(menuIndex==1)game.openHowTo();else if(menuIndex==2)disposeGame();}case PAUSED->{switch(menuIndex){case 0->game.resume();case 1->game.restartLevel();case 2->game.openHowTo();case 3->{game.returnToMenu();menuIndex=0;}case 4->disposeGame();}}case HOW_TO_PLAY->{game.closeHowTo();}case TRANSITION->game.continueAfterTransition();case VICTORY->{game.returnToMenu();menuIndex=0;}default->{}}}
        @Override public void keyPressed(KeyEvent e){int k=e.getKeyCode();if(k<keys.length)keys[k]=true;if(k==KeyEvent.VK_J&&game.state()==GameModel.State.PLAYING){game.toggleJournal();repaint();return;}if(k==KeyEvent.VK_ESCAPE){if(game.dialogueOpen()){game.closeDialogue();}else if(game.journalOpen()){game.closeJournal();}else if(game.state()==GameModel.State.PLAYING){game.togglePause();menuIndex=0;}else if(game.state()==GameModel.State.PAUSED)game.resume();else if(game.state()==GameModel.State.HOW_TO_PLAY){game.closeHowTo();}else if(game.state()==GameModel.State.MENU)disposeGame();}
            if(k==KeyEvent.VK_UP||k==KeyEvent.VK_W){menuIndex=Math.max(0,menuIndex-1);}if(k==KeyEvent.VK_DOWN||k==KeyEvent.VK_S){menuIndex=Math.min(game.state()==GameModel.State.PAUSED?4:2,menuIndex+1);}if(k==KeyEvent.VK_ENTER||k==KeyEvent.VK_SPACE){if(game.dialogueOpen()){game.closeDialogue();}else if(game.journalOpen()){game.closeJournal();}else if(game.state()==GameModel.State.PLAYING){game.update(0,0,0,true);}else activateMenu();}repaint();}
        @Override public void keyReleased(KeyEvent e){int k=e.getKeyCode();if(k<keys.length)keys[k]=false;}
        @Override public void keyTyped(KeyEvent e){}
        @Override public void mouseClicked(MouseEvent e){if(game.dialogueOpen()){game.closeDialogue();return;}if(game.journalOpen()){game.closeJournal();return;}if(game.state()==GameModel.State.PLAYING){game.update(0,0,0,true);return;}if(game.state()==GameModel.State.VICTORY){game.returnToMenu();menuIndex=0;return;}if(game.state()==GameModel.State.MENU){int y=e.getY();int start=getHeight()/2+42;int idx=(y-(start-29))/64;if(idx>=0&&idx<3){menuIndex=idx;activateMenu();}}else if(game.state()==GameModel.State.PAUSED){int idx=(e.getY()-(getHeight()/2-12-29))/43;if(idx>=0&&idx<5){menuIndex=idx;activateMenu();}}else if(game.state()==GameModel.State.TRANSITION)activateMenu();else if(game.state()==GameModel.State.HOW_TO_PLAY)activateMenu();requestFocusInWindow();repaint();}
        @Override public void mousePressed(MouseEvent e){}@Override public void mouseReleased(MouseEvent e){}@Override public void mouseEntered(MouseEvent e){}@Override public void mouseExited(MouseEvent e){}@Override public void mouseMoved(MouseEvent e){mouseX=e.getX();mouseY=e.getY();}@Override public void mouseDragged(MouseEvent e){}
    }
}

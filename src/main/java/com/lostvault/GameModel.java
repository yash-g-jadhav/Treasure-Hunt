package com.lostvault;

import java.util.*;

/** Pure gameplay model. Coordinates are world units with (0,0) at the upper-left. */
public final class GameModel {
    public enum State { MENU, PLAYING, PAUSED, HOW_TO_PLAY, TRANSITION, VICTORY }
    public enum Kind { GEM, CLUE, HAZARD, CHECKPOINT, GATE, EXIT, VAULT, TREASURE, BOAT, ROCK, TREE, RUIN }
    public static final int WORLD_W = 1800, WORLD_H = 1200, PLAYER_SIZE = 28;
    public static final double PLAYER_SPEED = 230.0;

    public static final class Item {
        public final String id, label; public final Kind kind; public final Rect bounds; public boolean active = true;
        public Item(String id, String label, Kind kind, double x, double y, double w, double h) { this.id=id; this.label=label; this.kind=kind; this.bounds=new Rect(x,y,w,h); }
    }
    public static final class Level {
        public final int number, gemRequirement; public final String name, subtitle; public final List<Item> items = new ArrayList<>();
        public final List<Rect> solids = new ArrayList<>(); public final UnlockCondition gateCondition; public final Set<String> clueIds;
        public Level(int n, String name, String subtitle, int req, Set<String> clueIds, UnlockCondition gate) { this.number=n;this.name=name;this.subtitle=subtitle;this.gemRequirement=req;this.clueIds=Set.copyOf(clueIds);this.gateCondition=gate; }
        public void item(String id,String label,Kind k,double x,double y,double w,double h){items.add(new Item(id,label,k,x,y,w,h));}
        public void solid(double x,double y,double w,double h){solids.add(new Rect(x,y,w,h));}
    }

    private State state = State.MENU;
    private State stateBeforeHowTo = State.MENU;
    private Level level;
    private int levelNumber = 1, lives = 3, levelGems = 0, totalGems = 0, checkpoint = 0;
    private double playerX = 100, playerY = 100, checkpointX = 100, checkpointY = 100, invulnerable = 0, animTime = 0;
    private String facing = "down", message = "", transitionText = "";
    private double messageTimer = 0;
    private final Set<String> collectedClues = new LinkedHashSet<>(), collectedGemIds = new LinkedHashSet<>();
    private final Map<Integer, Set<String>> runClues = new HashMap<>();
    private boolean gateUnlocked = false, vaultUnlocked = false, treasureSpawned = false, hasTreasure = false, transitionIssued = false;
    private int highestUnlocked = 1;

    public GameModel() { }
    public State state(){return state;} public Level level(){return level;} public int levelNumber(){return levelNumber;} public int lives(){return lives;}
    public int levelGems(){return levelGems;} public int totalGems(){return totalGems;} public int checkpoint(){return checkpoint;}
    public double playerX(){return playerX;} public double playerY(){return playerY;} public double animTime(){return animTime;} public String facing(){return facing;}
    public String message(){return message;} public String transitionText(){return transitionText;} public double messageTimer(){return messageTimer;}
    public Set<String> collectedClues(){return Collections.unmodifiableSet(collectedClues);} public boolean hasTreasure(){return hasTreasure;}
    public boolean gateUnlocked(){return gateUnlocked;} public boolean vaultUnlocked(){return vaultUnlocked;} public boolean treasureSpawned(){return treasureSpawned;}
    public int highestUnlocked(){return highestUnlocked;}
    public Rect playerBounds(){return new Rect(playerX,playerY,PLAYER_SIZE,PLAYER_SIZE);}

    public static double[] normalizedMovement(double dx, double dy) {
        double len=Math.hypot(dx,dy); if(len>1){dx/=len;dy/=len;} return new double[]{dx,dy};
    }
    public static boolean overlaps(Rect a, Rect b){return a.intersects(b);}

    public void startNewRun(){ totalGems=0; collectedGemIds.clear(); collectedClues.clear(); runClues.clear(); hasTreasure=false; highestUnlocked=1; startLevel(1); state=State.PLAYING; }
    public void startLevel(int n){
        if(n<1||n>3||n>highestUnlocked) throw new IllegalArgumentException("Level is locked or invalid: "+n);
        levelNumber=n; level=createLevel(n); lives=3; levelGems=0; checkpoint=0; checkpointX=100;checkpointY=100;playerX=100;playerY=100;
        collectedClues.clear();
        if(n==3){ for(Set<String> saved:runClues.values()) collectedClues.addAll(saved); }
        else collectedClues.addAll(runClues.getOrDefault(n,Set.of()));
        gateUnlocked=false;vaultUnlocked=false;treasureSpawned=false;transitionIssued=false;invulnerable=0;
        collectedGemIds.removeIf(id -> id.startsWith("L"+n+"-"));
        for(Item i:level.items) if(i.kind==Kind.GEM && collectedGemIds.contains(i.id)) {i.active=false;levelGems++;}
        message=level.subtitle;messageTimer=4;
    }
    private Level createLevel(int n){
        Level l;
        if(n==1){
            l=new Level(1,"THE LANDING SHORE","Find three gems, recover Clue A, then reach the jungle path.",3,Set.of("A"),new UnlockCondition(Set.of(),0));
            l.item("L1-G1","Sunlit gem",Kind.GEM,320,170,22,22);l.item("L1-G2","Tide gem",Kind.GEM,520,410,22,22);l.item("L1-G3","Palm gem",Kind.GEM,810,240,22,22);l.item("L1-G4","Hidden gem",Kind.GEM,1060,570,22,22);l.item("L1-A","Clue A",Kind.CLUE,660,680,28,30);
            l.item("L1-C1","Shore camp",Kind.CHECKPOINT,440,700,34,34);l.item("L1-C2","Jungle marker",Kind.CHECKPOINT,960,380,34,34);l.item("L1-EXIT","Jungle path",Kind.EXIT,1640,1040,70,90);
            l.item("L1-H1","Sharp coral",Kind.HAZARD,710,460,65,55);
            l.solid(260,270,110,42);l.solid(430,150,42,160);l.solid(630,350,125,42);l.solid(900,570,45,140);l.solid(1130,260,170,44);l.solid(1340,650,50,150);
            addTree(l,180,450);addTree(l,1200,450);addTree(l,1450,280);addRock(l,350,900);addRock(l,1250,900);
        } else if(n==2){
            l=new Level(2,"THE OVERGROWN INTERIOR","Find five gems and Clues B + C. The ancient gate needs both clues.",5,Set.of("B","C"),new UnlockCondition(Set.of("B","C"),0));
            double[][] gems={{260,220},{410,500},{620,280},{780,690},{1000,390},{1230,650},{1450,350}};int gi=1;for(double[] p:gems){l.item("L2-G"+gi,"Jungle gem",Kind.GEM,p[0],p[1],22,22);gi++;}
            l.item("L2-B","Clue B",Kind.CLUE,500,760,28,30);l.item("L2-C","Clue C",Kind.CLUE,1190,250,28,30);
            l.item("L2-C1","Ruined shrine",Kind.CHECKPOINT,570,400,34,34);l.item("L2-C2","Stone circle",Kind.CHECKPOINT,1330,780,34,34);
            l.item("L2-H1","Quicksand",Kind.HAZARD,870,400,105,80);l.item("L2-H2","Spike trap",Kind.HAZARD,1080,830,75,48);
            l.item("L2-GATE","Ancient gate",Kind.GATE,1510,780,54,100);l.item("L2-EXIT","Vault trail",Kind.EXIT,1660,1030,70,90);l.solid(1510,780,54,100);
            l.solid(230,330,170,42);l.solid(460,190,48,210);l.solid(660,460,170,45);l.solid(940,210,50,200);l.solid(1050,560,180,45);l.solid(1330,440,48,170);l.solid(1380,900,140,42);
            for(int i=0;i<8;i++)addTree(l,180+i*180,(i%2==0)?900:140);addRock(l,740,900);addRock(l,1220,970);
        } else {
            l=new Level(3,"VAULT ISLAND","The old clues reveal the Vault. Take the treasure and signal the rescue boat.",4,Set.of("A","B","C"),new UnlockCondition(Set.of("A","B","C"),0));
            double[][] gems={{260,240},{440,600},{700,300},{880,780},{1120,250},{1280,600}};int gi=1;for(double[] p:gems){l.item("L3-G"+gi,"Vault gem",Kind.GEM,p[0],p[1],22,22);gi++;}
            l.item("L3-C1","Broken obelisk",Kind.CHECKPOINT,520,370,34,34);l.item("L3-C2","Harbor marker",Kind.CHECKPOINT,1390,760,34,34);
            l.item("L3-H1","Cracked ground",Kind.HAZARD,980,480,95,70);
            l.item("L3-VAULT","The Lost Vault",Kind.VAULT,1280,340,100,115);l.item("L3-BOAT","Rescue boat",Kind.BOAT,1580,930,100,70);
            l.solid(250,360,180,44);l.solid(500,180,50,145);l.solid(640,550,180,44);l.solid(920,220,48,180);l.solid(1100,650,190,44);l.solid(1450,500,48,180);
            addTree(l,180,780);addTree(l,760,950);addRock(l,1080,870);addRock(l,1460,250);
        }
        // All worlds are enclosed; visual shore/water borders are handled by world bounds.
        return l;
    }
    private void addTree(Level l,double x,double y){l.item("decor-tree-"+x+"-"+y,"Tree",Kind.TREE,x,y,54,74);l.solid(x+15,y+38,24,30);}
    private void addRock(Level l,double x,double y){l.item("decor-rock-"+x+"-"+y,"Rock",Kind.ROCK,x,y,50,36);l.solid(x,y,50,36);}

    public void update(double dt,double dx,double dy,boolean interact){
        if(state!=State.PLAYING)return;dt=Math.max(0,Math.min(.05,dt));animTime+=dt;if(messageTimer>0)messageTimer-=dt;if(invulnerable>0)invulnerable-=dt;
        double[] v=normalizedMovement(dx,dy);dx=v[0];dy=v[1];if(Math.abs(dx)+Math.abs(dy)>0){if(Math.abs(dx)>Math.abs(dy))facing=dx<0?"left":"right";else facing=dy<0?"up":"down";}
        moveAxis(dx*PLAYER_SPEED*dt,0);moveAxis(0,dy*PLAYER_SPEED*dt);
        playerX=Math.max(12,Math.min(WORLD_W-PLAYER_SIZE-12,playerX));playerY=Math.max(12,Math.min(WORLD_H-PLAYER_SIZE-12,playerY));
        Rect p=playerBounds();
        for(Item i:level.items){if(!i.active||!p.intersects(i.bounds))continue;
            switch(i.kind){case GEM -> collectGem(i);case CLUE -> collectClue(i);case HAZARD -> hitHazard();case CHECKPOINT -> activateCheckpoint(i);case GATE -> {if(gateUnlocked||level.gateCondition.isMet(collectedClues,levelGems)){gateUnlocked=true;i.active=false;level.solids.remove(new Rect(i.bounds.x(),i.bounds.y(),i.bounds.w(),i.bounds.h()));message("The ancient gate grinds open.");}else message("The gate needs Clues B and C.");}case EXIT -> {if(canExit()){if(!transitionIssued){transitionIssued=true;transitionText=level.name+" COMPLETE";state=State.TRANSITION;highestUnlocked=Math.max(highestUnlocked,Math.min(3,levelNumber+1));}}else if(messageTimer<=0)message(exitRequirement());}case VAULT -> {if(level.gateCondition.isMet(collectedClues,levelGems)){if(!vaultUnlocked){vaultUnlocked=true;message("The Lost Vault unlocks with a deep rumble!");}}else message("The Vault answers to Clues A, B and C.");}case TREASURE -> {hasTreasure=true;i.active=false;message("Treasure secured! Reach the rescue boat.");}case BOAT -> {if(hasTreasure&&interact){state=State.VICTORY;message="You escaped with the Lost Vault treasure!";}else message("The boat is waiting. Find the treasure first, then press Space.");}default -> {}}
        }
        if(interact) interactNearby();
    }
    private void moveAxis(double dx,double dy){double nx=playerX+dx,ny=playerY+dy;Rect candidate=new Rect(nx,ny,PLAYER_SIZE,PLAYER_SIZE);for(Rect s:level.solids)if(candidate.intersects(s)){if(dx>0)nx=s.x()-PLAYER_SIZE;else if(dx<0)nx=s.right();if(dy>0)ny=s.y()-PLAYER_SIZE;else if(dy<0)ny=s.bottom();candidate=new Rect(nx,ny,PLAYER_SIZE,PLAYER_SIZE);}playerX=nx;playerY=ny;}
    private void collectGem(Item i){i.active=false;if(collectedGemIds.add(i.id)){levelGems++;totalGems++;message("Gem collected  +1");}}
    private void collectClue(Item i){i.active=false;if(collectedClues.add(i.id.substring(i.id.lastIndexOf('-')+1))){runClues.put(levelNumber,new LinkedHashSet<>(collectedClues));message(i.label+" recovered: " + clueText(i.id));}}
    private String clueText(String id){return switch(id){case "L1-A"->"Three marks point toward the overgrown ruins.";case "L2-B"->"The Vault remembers the first mark.";case "L2-C"->"Three marks, one key, one way home.";default->"A fragment of the island's history.";};}
    private void hitHazard(){if(invulnerable>0)return;lives--;invulnerable=1.5;message("Hazard hit! Lives remaining: "+lives);if(lives<=0){runClues.remove(levelNumber);startLevel(levelNumber);state=State.PLAYING;message("Out of lives — this level attempt has restarted.");}else{playerX=checkpointX;playerY=checkpointY;}}
    private void activateCheckpoint(Item i){if(i.id.equals("L"+levelNumber+"-C1")){checkpoint=1;}else if(i.id.equals("L"+levelNumber+"-C2")){checkpoint=2;}checkpointX=i.bounds.x();checkpointY=i.bounds.y();message("Checkpoint secured.");i.active=false;}
    private boolean canExit(){if(levelNumber==1)return levelGems>=3&&collectedClues.contains("A");if(levelNumber==2)return levelGems>=5&&collectedClues.containsAll(Set.of("B","C"))&&gateUnlocked;return levelGems>=4&&vaultUnlocked&&hasTreasure;}
    private String exitRequirement(){return switch(levelNumber){case 1->"Need 3 gems and Clue A to follow the trail.";case 2->"Need 5 gems, Clues B + C, and the open gate.";default->"Unlock the Vault, collect 4 gems, and secure the treasure.";};}
    private void interactNearby(){
        if(level==null)return;
        Rect near=new Rect(playerX-24,playerY-24,PLAYER_SIZE+48,PLAYER_SIZE+48);
        Item nearby=null;
        for(Item i:level.items){if(i.active&&near.intersects(i.bounds)&&(i.kind==Kind.VAULT||i.kind==Kind.BOAT||i.kind==Kind.GATE)){nearby=i;break;}}
        if(nearby==null)return;
        if(nearby.kind==Kind.VAULT){
            if(level.gateCondition.isMet(collectedClues,levelGems)){
                vaultUnlocked=true;
                if(!treasureSpawned){treasureSpawned=true;level.item("L3-TREASURE","Ancient treasure",Kind.TREASURE,nearby.bounds.x()+35,nearby.bounds.y()+80,30,30);}
                message("Vault unlocked. Take the treasure!");
            }else message("The Vault needs Clues A, B and C.");
        }else if(nearby.kind==Kind.BOAT){if(hasTreasure)state=State.VICTORY;else message("The rescue boat needs the treasure first.");}
        else if(nearby.kind==Kind.GATE){
            if(level.gateCondition.isMet(collectedClues,levelGems)){gateUnlocked=true;nearby.active=false;level.solids.remove(new Rect(nearby.bounds.x(),nearby.bounds.y(),nearby.bounds.w(),nearby.bounds.h()));message("The gate is open.");}
            else message("The gate is sealed. Find both clues.");
        }
    }
    public void togglePause(){if(state==State.PLAYING)state=State.PAUSED;else if(state==State.PAUSED)state=State.PLAYING;}
    public void openHowTo(){stateBeforeHowTo=state;state=State.HOW_TO_PLAY;}
    public void closeHowTo(){state=stateBeforeHowTo;}
    public void resume(){if(state==State.PAUSED)state=State.PLAYING;}
    public void restartLevel(){runClues.remove(levelNumber);startLevel(levelNumber);state=State.PLAYING;}
    public void returnToMenu(){state=State.MENU;level=null;}
    public void continueAfterTransition(){if(state!=State.TRANSITION)return;if(levelNumber<3){startLevel(levelNumber+1);state=State.PLAYING;}else state=State.VICTORY;}
    public void message(String text){message=text;messageTimer=2.4;}
    public boolean isNearActive(Kind kind){if(level==null)return false;Rect near=new Rect(playerX-24,playerY-24,PLAYER_SIZE+48,PLAYER_SIZE+48);return level.items.stream().anyMatch(i->i.active&&i.kind==kind&&near.intersects(i.bounds));}
    public boolean hasClue(String clue){return collectedClues.contains(clue);}

    // Package-private deterministic hooks used only by the headless regression suite.
    void teleportForTest(double x,double y){playerX=x;playerY=y;}
    void setCheckpointForTest(double x,double y){checkpointX=x;checkpointY=y;checkpoint=1;}
    void forceLivesForTest(int value){lives=value;invulnerable=0;}
    void debugStartLevel(int n){highestUnlocked=3;startLevel(n);state=State.PLAYING;}
    void debugSetClues(Set<String> clues){collectedClues.clear();collectedClues.addAll(clues);runClues.put(levelNumber,new LinkedHashSet<>(clues));}
    void debugSetTreasure(boolean value){hasTreasure=value;}
}

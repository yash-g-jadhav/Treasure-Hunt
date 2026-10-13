package com.lostvault;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Pure gameplay model. Coordinates are world units with (0,0) at the upper-left. */
public final class GameModel {
    public enum State { MENU, PLAYING, PAUSED, HOW_TO_PLAY, TRANSITION, VICTORY }
    public enum Kind { GEM, CLUE, HAZARD, CHECKPOINT, GATE, EXIT, VAULT, TREASURE, BOAT, ROCK, TREE, RUIN, NPC, INSCRIPTION, KEY, PUZZLE, SECRET, CACHE, SWITCH }
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
    private String facing = "down", message = "", transitionText = "", dialogueTitle = "", dialogueText = "";
    private boolean dialogueOpen = false, journalOpen = false, inventoryOpen = false, mapOpen = false, puzzleOpen = false;
    private String dialogueContext = "", puzzleInput = "", ending = "";
    private String[] dialogueChoices = new String[0];
    private int dialogueChoice = 0;
    private final Set<String> inventory = new LinkedHashSet<>(), exploredCells = new LinkedHashSet<>();
    private boolean gatePuzzleSolved = false, vaultPuzzleSolved = false, secretRoomRevealed = false;
    private static final Path SAVE_PATH = Paths.get(System.getProperty("user.home", "."), ".the-lost-vault", "save.properties");
    private final List<String> journalEntries = new ArrayList<>();
    private double messageTimer = 0, escapeTimer = 0;
    private final Set<String> collectedClues = new LinkedHashSet<>(), collectedGemIds = new LinkedHashSet<>();
    private final Map<Integer, Set<String>> runClues = new HashMap<>();
    private boolean gateUnlocked = false, vaultUnlocked = false, treasureSpawned = false, hasTreasure = false, transitionIssued = false;
    private int highestUnlocked = 1;

    public GameModel() { highestUnlocked=loadHighestUnlocked(); }
    public State state(){return state;} public Level level(){return level;} public int levelNumber(){return levelNumber;} public int lives(){return lives;}
    public int levelGems(){return levelGems;} public int totalGems(){return totalGems;} public int checkpoint(){return checkpoint;}
    public double playerX(){return playerX;} public double playerY(){return playerY;} public double animTime(){return animTime;} public String facing(){return facing;}
    public String message(){return message;} public String transitionText(){return transitionText;} public double messageTimer(){return messageTimer;}
    public String objective(){if(level==null)return "";return switch(levelNumber){case 1->!collectedClues.contains("A")?"Find Clue A; recover 3 gems to reach the jungle.":levelGems<3?"Clue A found. Recover "+(3-levelGems)+" more gem(s), then follow the inland path.":"The shore is charted. Reach the inland path.";case 2->!collectedClues.containsAll(Set.of("B","C"))?"Search the ruins for the Thorn and Sun clues.":!gatePuzzleSolved?"Solve the stone dial: THORN → SUN (2, 3).":"Open the ancient gate and reach the eastern exit.";default->!collectedClues.containsAll(Set.of("A","B","C"))?"Reconstruct the Wave, Thorn, and Sun oath.":!vaultPuzzleSolved?"Solve the crown dial: WAVE → THORN → SUN (1, 2, 3).":!hasTreasure?"Open the Vault and recover the crown.":"Escape east to the rescue boat.";};}
    public Set<String> collectedClues(){return Collections.unmodifiableSet(collectedClues);} public boolean hasTreasure(){return hasTreasure;} public double escapeTimer(){return escapeTimer;}
    public boolean gateUnlocked(){return gateUnlocked;} public boolean vaultUnlocked(){return vaultUnlocked;} public boolean treasureSpawned(){return treasureSpawned;}
    public int highestUnlocked(){return Math.max(highestUnlocked,loadHighestUnlocked());} public boolean hasSavedProgress(){return loadHighestUnlocked()>1;}
    public boolean dialogueOpen(){return dialogueOpen;} public boolean journalOpen(){return journalOpen;}
    public boolean inventoryOpen(){return inventoryOpen;} public boolean mapOpen(){return mapOpen;} public boolean puzzleOpen(){return puzzleOpen;}
    public String puzzleInput(){return puzzleInput;} public String ending(){return ending;} public boolean gatePuzzleSolved(){return gatePuzzleSolved;} public boolean vaultPuzzleSolved(){return vaultPuzzleSolved;}
    public Set<String> inventory(){return Collections.unmodifiableSet(inventory);} public Set<String> exploredCells(){return Collections.unmodifiableSet(exploredCells);}
    public String[] dialogueChoices(){return dialogueChoices.clone();} public int dialogueChoice(){return dialogueChoice;}
    public String dialogueTitle(){return dialogueTitle;} public String dialogueText(){return dialogueText;}
    public List<String> journalEntries(){return Collections.unmodifiableList(journalEntries);}
    public Rect playerBounds(){return new Rect(playerX,playerY,PLAYER_SIZE,PLAYER_SIZE);}

    public static double[] normalizedMovement(double dx, double dy) {
        double len=Math.hypot(dx,dy); if(len>1){dx/=len;dy/=len;} return new double[]{dx,dy};
    }
    public static boolean overlaps(Rect a, Rect b){return a.intersects(b);}

    public void startNewRun(){ totalGems=0; collectedGemIds.clear(); collectedClues.clear(); runClues.clear(); journalEntries.clear(); inventory.clear(); exploredCells.clear(); hasTreasure=false; highestUnlocked=1; dialogueOpen=false; journalOpen=false; inventoryOpen=false; mapOpen=false; puzzleOpen=false; gatePuzzleSolved=false;vaultPuzzleSolved=false;secretRoomRevealed=false;ending=""; startLevel(1); state=State.PLAYING; message("A storm has wrecked your ship. Someone left a trail for you..."); journal("EXPEDITION BEGINS — Find out why Captain Elias Vale sailed into the storm."); }
    public void continueSavedRun(){highestUnlocked=Math.max(1,loadHighestUnlocked());if(highestUnlocked<=1){startNewRun();return;}inventory.clear();collectedClues.clear();collectedClues.addAll(loadSavedClues());for(String clue:collectedClues)runClues.computeIfAbsent(1,k->new LinkedHashSet<>()).add(clue);startLevel(highestUnlocked);if(highestUnlocked==3)collectedClues.addAll(Set.of("A","B","C"));state=State.PLAYING;message("Expedition resumed. Your map knowledge survived the crossing.");journal("Expedition resumed from the last unlocked chapter.");}
    private int loadHighestUnlocked(){Properties p=new Properties();try(InputStream in=Files.newInputStream(SAVE_PATH)){p.load(in);int n=Integer.parseInt(p.getProperty("highestUnlocked","1"));return Math.max(1,Math.min(3,n));}catch(Exception ignored){return 1;}}
    private Set<String> loadSavedClues(){Properties p=new Properties();try(InputStream in=Files.newInputStream(SAVE_PATH)){p.load(in);String v=p.getProperty("clues","");return v.isBlank()?Set.of():new LinkedHashSet<>(Arrays.asList(v.split(",")));}catch(Exception ignored){return Set.of();}}
    private void saveProgress(){try{Files.createDirectories(SAVE_PATH.getParent());Properties p=new Properties();p.setProperty("highestUnlocked",Integer.toString(highestUnlocked));Set<String> allClues=new LinkedHashSet<>();for(Set<String> saved:runClues.values())allClues.addAll(saved);allClues.addAll(collectedClues);p.setProperty("clues",String.join(",",allClues));try(OutputStream out=Files.newOutputStream(SAVE_PATH)){p.store(out,"The Lost Vault expedition progress");}}catch(IOException ex){System.err.println("Could not save expedition progress: "+ex.getMessage());}}
    public void startLevel(int n){
        if(n<1||n>3||n>highestUnlocked) throw new IllegalArgumentException("Level is locked or invalid: "+n);
        levelNumber=n; level=createLevel(n); lives=3; levelGems=0; checkpoint=0; checkpointX=100;checkpointY=100;playerX=100;playerY=100;
        collectedClues.clear();
        if(n==3){ for(Set<String> saved:runClues.values()) collectedClues.addAll(saved); }
        else collectedClues.addAll(runClues.getOrDefault(n,Set.of()));
        gateUnlocked=false;vaultUnlocked=false;treasureSpawned=false;transitionIssued=false;invulnerable=0;gatePuzzleSolved=false;vaultPuzzleSolved=false;puzzleOpen=false;dialogueOpen=false;inventoryOpen=false;mapOpen=false;exploredCells.clear();hasTreasure=false;escapeTimer=0;
        collectedGemIds.removeIf(id -> id.startsWith("L"+n+"-"));
        for(Item i:level.items) if(i.kind==Kind.GEM && collectedGemIds.contains(i.id)) {i.active=false;levelGems++;}
        message=level.subtitle;messageTimer=4;
    }
    private Level createLevel(int n){
        Level l;
        if(n==1){
            l=new Level(1,"THE LANDING SHORE","Find three gems, recover Clue A, then reach the jungle path.",3,Set.of("A"),new UnlockCondition(Set.of(),0));
            l.item("L1-G1","Sunlit gem",Kind.GEM,320,170,22,22);l.item("L1-G2","Tide gem",Kind.GEM,520,410,22,22);l.item("L1-G3","Palm gem",Kind.GEM,810,240,22,22);l.item("L1-G4","Hidden gem",Kind.GEM,1060,570,22,22);l.item("L1-A","Clue A",Kind.CLUE,660,680,28,30);
            l.item("L1-C1","Shore camp",Kind.CHECKPOINT,440,700,34,34);l.item("L1-C2","Jungle marker",Kind.CHECKPOINT,960,380,34,34);l.item("L1-NPC","Mara the castaway",Kind.NPC,185,155,34,44);l.item("L1-I1","Carved driftwood",Kind.INSCRIPTION,740,760,34,34);l.item("L1-EXIT","Jungle path",Kind.EXIT,1640,1040,70,90);
            l.item("L1-H1","Sharp coral",Kind.HAZARD,710,460,65,55);
            l.item("L1-SECRET","Suspicious vine-covered wall",Kind.SECRET,320,730,42,58);
            l.item("L1-CACHE","Smuggler cache",Kind.CACHE,365,750,34,30); l.items.get(l.items.size()-1).active=false;
            l.item("L1-SWITCH","Tidewheel",Kind.SWITCH,860,830,36,36);l.item("L1-TIDE-BLOCK","Collapsed tide barrier",Kind.RUIN,1325,640,70,150);
            l.solid(260,270,110,42);l.solid(430,150,42,160);l.solid(630,350,125,42);l.solid(900,570,45,140);l.solid(1130,260,170,44);l.solid(1340,650,50,150);
            addTree(l,180,450);addTree(l,1200,450);addTree(l,1450,280);addRock(l,350,900);addRock(l,1250,900);
        } else if(n==2){
            l=new Level(2,"THE OVERGROWN INTERIOR","Find five gems and Clues B + C. The ancient gate needs both clues.",5,Set.of("B","C"),new UnlockCondition(Set.of("B","C"),0));
            double[][] gems={{260,220},{410,500},{620,280},{780,690},{1000,390},{1230,650},{1450,350}};int gi=1;for(double[] p:gems){l.item("L2-G"+gi,"Jungle gem",Kind.GEM,p[0],p[1],22,22);gi++;}
            l.item("L2-B","Clue B",Kind.CLUE,500,760,28,30);l.item("L2-C","Clue C",Kind.CLUE,1190,250,28,30);l.item("L2-NPC","The Lost Cartographer",Kind.NPC,720,790,34,44);l.item("L2-I1","Stone inscription",Kind.INSCRIPTION,1010,700,34,34);
            l.item("L2-C1","Ruined shrine",Kind.CHECKPOINT,570,400,34,34);l.item("L2-C2","Stone circle",Kind.CHECKPOINT,1330,880,34,34);
            l.item("L2-H1","Quicksand",Kind.HAZARD,870,400,105,80);
            l.item("L2-PUZZLE","Three-mark stone dial",Kind.PUZZLE,1100,620,48,48);
            l.item("L2-SECRET","Cracked ruin wall",Kind.SECRET,760,970,42,58);
            l.item("L2-CACHE","Ruins cache",Kind.CACHE,815,990,34,30); l.items.get(l.items.size()-1).active=false;
            l.item("L2-SWITCH","Sunken lever",Kind.SWITCH,1260,540,34,38);l.item("L2-LINTEL","Collapsed stone lintel",Kind.RUIN,1370,870,150,60);l.item("L2-H2","Spike trap",Kind.HAZARD,1080,830,75,48);
            l.item("L2-GATE","Ancient gate",Kind.GATE,1510,880,54,100);l.item("L2-EXIT","Vault trail",Kind.EXIT,1580,1000,70,90);l.solid(1510,880,54,100);
            l.solid(230,330,170,42);l.solid(460,190,48,210);l.solid(660,460,170,45);l.solid(940,210,50,200);l.solid(1050,560,180,45);l.solid(1330,440,48,170);l.solid(1380,900,140,42);
            for(int i=0;i<8;i++)addTree(l,180+i*180,(i%2==0)?900:140);addRock(l,740,900);addRock(l,1220,970);
        } else {
            l=new Level(3,"VAULT ISLAND","The old clues reveal the Vault. Take the treasure and signal the rescue boat.",4,Set.of("A","B","C"),new UnlockCondition(Set.of("A","B","C"),0));
            double[][] gems={{260,240},{440,600},{700,300},{880,780},{1120,250},{1280,600}};int gi=1;for(double[] p:gems){l.item("L3-G"+gi,"Vault gem",Kind.GEM,p[0],p[1],22,22);gi++;}
            l.item("L3-C1","Broken obelisk",Kind.CHECKPOINT,520,370,34,34);l.item("L3-C2","Harbor marker",Kind.CHECKPOINT,1390,760,34,34);l.item("L3-NPC","The Keeper",Kind.NPC,1060,330,34,44);l.item("L3-I1","Keeper inscription",Kind.INSCRIPTION,1150,500,34,34);
            l.item("L3-H1","Cracked ground",Kind.HAZARD,980,480,95,70);
            l.item("L3-PUZZLE","Crown constellation dial",Kind.PUZZLE,1020,590,48,48);
            l.item("L3-SECRET","False stone panel",Kind.SECRET,700,920,42,58);
            l.item("L3-CACHE","Keeper's cache",Kind.CACHE,755,940,34,30); l.items.get(l.items.size()-1).active=false;
            l.item("L3-VAULT","The Lost Vault",Kind.VAULT,1280,340,100,115);l.item("L3-BOAT","Rescue boat",Kind.BOAT,1550,1010,100,70);
            l.solid(250,360,180,44);l.solid(500,180,50,145);l.solid(640,550,180,44);l.solid(920,220,48,180);l.solid(1100,650,190,44);l.solid(1450,500,48,180);
            addTree(l,180,780);addTree(l,760,950);addRock(l,1080,870);addRock(l,1460,250);
        }
        // All worlds are enclosed; visual shore/water borders are handled by world bounds.
        return l;
    }
    private void addTree(Level l,double x,double y){l.item("decor-tree-"+x+"-"+y,"Tree",Kind.TREE,x,y,54,74);l.solid(x+15,y+38,24,30);}
    private void addRock(Level l,double x,double y){l.item("decor-rock-"+x+"-"+y,"Rock",Kind.ROCK,x,y,50,36);l.solid(x,y,50,36);}

    public void update(double dt,double dx,double dy,boolean interact){
        if(state!=State.PLAYING||dialogueOpen||journalOpen||inventoryOpen||mapOpen||puzzleOpen)return;dt=Math.max(0,Math.min(.05,dt));animTime+=dt;if(messageTimer>0)messageTimer-=dt;if(invulnerable>0)invulnerable-=dt;if(hasTreasure&&escapeTimer>0){escapeTimer-=dt;if(escapeTimer<=0){if(lives>1){lives--;playerX=checkpointX;playerY=checkpointY;invulnerable=1.5;escapeTimer=20;message("The island collapses around you! A life is lost; escape in 20 seconds!");}else{lives=0;startLevel(levelNumber);state=State.PLAYING;message("The island swallowed the crown. The Vault expedition has restarted.");}}}
        double[] v=normalizedMovement(dx,dy);dx=v[0];dy=v[1];if(Math.abs(dx)+Math.abs(dy)>0){if(Math.abs(dx)>Math.abs(dy))facing=dx<0?"left":"right";else facing=dy<0?"up":"down";}
        moveAxis(dx*PLAYER_SPEED*dt,0);moveAxis(0,dy*PLAYER_SPEED*dt);
        discoverAroundPlayer();
        playerX=Math.max(12,Math.min(WORLD_W-PLAYER_SIZE-12,playerX));playerY=Math.max(12,Math.min(WORLD_H-PLAYER_SIZE-12,playerY));
        Rect p=playerBounds();
        for(Item i:level.items){if(!i.active||!p.intersects(i.bounds))continue;
            switch(i.kind){
                case GEM -> collectGem(i);
                case HAZARD -> hitHazard();
                case CHECKPOINT -> activateCheckpoint(i);
                case EXIT -> {if(canExit()){if(!transitionIssued){transitionIssued=true;transitionText=level.name+" COMPLETE";state=State.TRANSITION;highestUnlocked=Math.max(highestUnlocked,Math.min(3,levelNumber+1));saveProgress();}}else if(messageTimer<=0)message(exitRequirement());}
                default -> { /* Interactable objects are handled once by interactNearby below. */ }
            }
        }
        if(interact&&!dialogueOpen) interactNearby();
    }
    private void discoverAroundPlayer(){int cx=(int)(playerX/120),cy=(int)(playerY/120);for(int x=cx-1;x<=cx+1;x++)for(int y=cy-1;y<=cy+1;y++)if(x>=0&&y>=0&&x<15&&y<10)exploredCells.add(x+","+y);}
    private void activateSwitch(Item i){i.active=false;if(i.id.equals("L1-SWITCH")){level.solids.remove(new Rect(1340,650,50,150));for(Item wall:level.items)if(wall.id.equals("L1-TIDE-BLOCK"))wall.active=false;message("The tidewheel turns. A sandbar opens the eastern branch.");journal("TIDEWHEEL — The builders used the tide as a moving bridge. The mechanism confirms this island is engineered, not cursed.");}else if(i.id.equals("L2-SWITCH")){level.solids.remove(new Rect(1380,900,140,42));for(Item wall:level.items)if(wall.id.equals("L2-LINTEL"))wall.active=false;message("A hidden stairway opens beneath the vines.");journal("The sunken lever removes the collapsed lintel and reveals a service route through the ruins.");}else message("The mechanism clicks into place.");}
    private void revealSecret(Item i){if(i.id.equals("L1-SECRET")&&!inventory.contains("Rusty key")){message("The vines hide a keyhole. Mara may know what fits here.");return;}if(i.id.equals("L2-SECRET")&&!collectedClues.contains("B")){message("The wall has a thorn-shaped groove. Find the Thorn clue first.");return;}i.active=false;String cacheId="L"+levelNumber+"-CACHE";for(Item item:level.items)if(item.id.equals(cacheId))item.active=true;secretRoomRevealed=true;message("SECRET FOUND — A concealed chamber opens!");journal("Secret chamber discovered in "+level.name+". Exploration has uncovered a route the main trail never reveals.");}
    private void openCache(Item i){if(!i.active)return;i.active=false;String reward=levelNumber==1?"Captain's log":levelNumber==2?"Cartographer's compass":"Keeper's Seal";inventory.add(reward);if(levelNumber==1){level.item("L1-G5","Cache gem",Kind.GEM,i.bounds.x()+42,i.bounds.y()+8,22,22);}message("Cache opened: "+reward+" acquired.");journal("Secret cache reward: "+reward+". This item may change how the island's mechanisms respond.");}
    private void openPuzzle(Item i){if(i.id.equals("L2-PUZZLE")){if(!collectedClues.containsAll(Set.of("B","C"))){message("The dial needs the Thorn and Sun clues.");return;}puzzleOpen=true;puzzleInput="";message("Stone dial: enter THORN then SUN with keys 2, 3.");}else if(i.id.equals("L3-PUZZLE")){if(!collectedClues.containsAll(Set.of("A","B","C"))){message("The constellation needs the Wave, Thorn, and Sun clues.");return;}puzzleOpen=true;puzzleInput="";message("Crown dial: enter WAVE, THORN, SUN with keys 1, 2, 3.");}}
    public void enterPuzzleSymbol(int symbol){if(!puzzleOpen)return;String expected=levelNumber==2?"23":"123";puzzleInput+=symbol;if(!expected.startsWith(puzzleInput)){puzzleInput="";message("The symbols reject the sequence. The dial resets.");return;}if(puzzleInput.equals(expected)){puzzleOpen=false;puzzleInput="";if(levelNumber==2){gatePuzzleSolved=true;inventory.add("Stone dial token");message("PUZZLE SOLVED — The Thorn and Sun align. The gate's lock disengages.");journal("Stone dial solved: Thorn → Sun. The ancient gate is now receptive to the two marks.");}else{vaultPuzzleSolved=true;inventory.add("Keeper's Seal");message("PUZZLE SOLVED — The Vault recognizes the oath.");journal("Crown constellation solved: Wave → Thorn → Sun. The Keeper's Seal is recovered.");}}else message("Symbol accepted: "+puzzleInput.length()+" / "+expected.length());}
    public void cancelPuzzle(){puzzleOpen=false;puzzleInput="";}
    private void moveAxis(double dx,double dy){double nx=playerX+dx,ny=playerY+dy;Rect candidate=new Rect(nx,ny,PLAYER_SIZE,PLAYER_SIZE);for(Rect s:level.solids)if(candidate.intersects(s)){if(dx>0)nx=s.x()-PLAYER_SIZE;else if(dx<0)nx=s.right();if(dy>0)ny=s.y()-PLAYER_SIZE;else if(dy<0)ny=s.bottom();candidate=new Rect(nx,ny,PLAYER_SIZE,PLAYER_SIZE);}playerX=nx;playerY=ny;}
    private void collectGem(Item i){i.active=false;if(collectedGemIds.add(i.id)){levelGems++;totalGems++;message("Gem collected  +1");}}
    private void collectClue(Item i){i.active=false;if(collectedClues.add(i.id.substring(i.id.lastIndexOf('-')+1))){runClues.put(levelNumber,new LinkedHashSet<>(collectedClues));String text=i.label+" recovered: "+clueText(i.id);message(text);journal(text);openDialogue(i.label,clueText(i.id));}}
    private String clueText(String id){return switch(id){case "L1-A"->"CLUE A — THE WAVE\n\nA charcoal chart marks the island as a grid of crossings. A wave is drawn at the western shore, a thorn at the old ruins, and a sun above the Vault. In the margin, Captain Vale wrote: 'The storm was summoned to keep the crown hidden. Trust the map, but inspect every branch.' The first symbol glows faintly when held toward the jungle.";case "L2-B"->"CLUE B — THE THORN\n\nThe cartographer's field note reads: 'The gate is a promise, not a lock. Wave and Thorn must stand together.' A sketch shows two paths splitting around a square of quicksand. The safe route bends north; the risky shortcut hides a gem. Someone has scratched V.ALE into the paper.";case "L2-C"->"CLUE C — THE SUN\n\nThe Sun mark completes the oath: 'No treasure belongs to one who leaves a friend behind.' The reverse side reveals a harbor symbol east of the Vault. Three tiny dots mark the checkpoints, and a final note warns: 'Once the chest is opened, do not retrace your steps. Take the eastern grid to the boat.'";default->"A fragment of the island's history. The symbols form a map when read in order.";};}
    private void journal(String entry){if(!journalEntries.contains(entry))journalEntries.add(entry);}
    private void openDialogue(String title,String text){dialogueTitle=title;dialogueText=text;dialogueOpen=true;}
    public void closeDialogue(){dialogueOpen=false;dialogueChoices=new String[0];dialogueContext="";}
    public void toggleJournal(){if(state==State.PLAYING&&!dialogueOpen&&!puzzleOpen)journalOpen=!journalOpen;}
    public void toggleInventory(){if(state==State.PLAYING&&!dialogueOpen&&!puzzleOpen)inventoryOpen=!inventoryOpen;}
    public void toggleMap(){if(state==State.PLAYING&&!dialogueOpen&&!puzzleOpen)mapOpen=!mapOpen;}
    public void closeInventory(){inventoryOpen=false;} public void closeMap(){mapOpen=false;}
    public void closeJournal(){journalOpen=false;}
    private void hitHazard(){if(invulnerable>0)return;lives--;invulnerable=1.5;message("Hazard hit! Lives remaining: "+lives);if(lives<=0){runClues.remove(levelNumber);startLevel(levelNumber);state=State.PLAYING;message("Out of lives — this level attempt has restarted.");}else{playerX=checkpointX;playerY=checkpointY;}}
    private void activateCheckpoint(Item i){if(i.id.equals("L"+levelNumber+"-C1")){checkpoint=1;}else if(i.id.equals("L"+levelNumber+"-C2")){checkpoint=2;}checkpointX=i.bounds.x();checkpointY=i.bounds.y();message("Checkpoint secured.");i.active=false;}
    private boolean canExit(){if(levelNumber==1)return levelGems>=3&&collectedClues.contains("A");if(levelNumber==2)return levelGems>=5&&collectedClues.containsAll(Set.of("B","C"))&&gatePuzzleSolved&&gateUnlocked;return levelGems>=4&&vaultUnlocked&&hasTreasure;}
    private String exitRequirement(){return switch(levelNumber){case 1->"Need 3 gems and Clue A to follow the trail.";case 2->"Need 5 gems, Clues B + C, and the open gate.";default->"Unlock the Vault, collect 4 gems, and secure the treasure.";};}
    private void interactNearby(){
        if(level==null)return;
        Rect near=new Rect(playerX-34,playerY-34,PLAYER_SIZE+68,PLAYER_SIZE+68);
        Item nearby=null;double nearest=Double.MAX_VALUE;
        for(Item i:level.items){if(i.active&&near.intersects(i.bounds)&&(i.kind==Kind.CLUE||i.kind==Kind.VAULT||i.kind==Kind.BOAT||i.kind==Kind.GATE||i.kind==Kind.NPC||i.kind==Kind.INSCRIPTION||i.kind==Kind.TREASURE||i.kind==Kind.KEY||i.kind==Kind.PUZZLE||i.kind==Kind.SECRET||i.kind==Kind.CACHE||i.kind==Kind.SWITCH)){double ox=i.bounds.x()+i.bounds.w()/2-(playerX+PLAYER_SIZE/2.0),oy=i.bounds.y()+i.bounds.h()/2-(playerY+PLAYER_SIZE/2.0);double distance=ox*ox+oy*oy;if(distance<nearest){nearest=distance;nearby=i;}}}
        if(nearby==null){message("Nothing nearby answers. Explore the paths and inspect anything that glows or bears a mark.");return;}
        switch(nearby.kind){
            case CLUE -> collectClue(nearby);
            case KEY -> {inventory.add(nearby.label);nearby.active=false;message(nearby.label+" added to inventory.");journal("Item recovered: "+nearby.label);}
            case PUZZLE -> openPuzzle(nearby);
            case SECRET -> revealSecret(nearby);
            case CACHE -> openCache(nearby);
            case SWITCH -> activateSwitch(nearby);
            case NPC -> talkTo(nearby);
            case INSCRIPTION -> inspectInscription(nearby);
            case VAULT -> {
                if(level.gateCondition.isMet(collectedClues,levelGems)){
                    if(!vaultPuzzleSolved){openDialogue("THE LOST VAULT", "The Wave, Thorn, and Sun fit the inscription—but the crown constellation still has to be entered. Find the stone dial nearby and enter 1, 2, 3 in that order.");}
                    else {vaultUnlocked=true;if(!treasureSpawned){treasureSpawned=true;level.item("L3-TREASURE","Ancient treasure",Kind.TREASURE,nearby.bounds.x()+35,nearby.bounds.y()+80,30,30);journal("The three marks align. The Lost Vault is open. The treasure rests inside—but the rescue boat will not wait forever.");}openDialogue("THE LOST VAULT", "The constellation recognizes your answer. Stone doors roll apart, revealing the crown. Take it, then escape by the eastern grid before the island collapses!");}
                }else openDialogue("SEALED VAULT", "Three marks are missing from the lock. The old oath says the Wave, Thorn, and Sun must be carried by the same explorer. Search every trail for the clue scrolls.");
            }
            case TREASURE -> {hasTreasure=true;escapeTimer=45;nearby.active=false;message("TREASURE CLAIMED! The island is collapsing — escape within 45 seconds!");journal("Treasure recovered. The ground shakes; the escape route is the rescue boat at the harbor.");openDialogue("THE PRICE OF GREED", "The chest snaps shut behind you. A deep crack tears through the island. You have the treasure—now RUN to the rescue boat!");}
            case BOAT -> {if(hasTreasure){state=State.VICTORY;ending=inventory.contains("Keeper's Promise")&&inventory.contains("Keeper's Seal")?"TRUE ENDING — The crown is returned to the people.":"TREASURE ENDING — You escaped with the crown, but its history remains unfinished.";message(ending);saveProgress();}else openDialogue("RESCUE BOAT", "The boat is seaworthy, but the treasure is still inside the Vault. You did not come this far to leave the mystery unsolved.");}
            case GATE -> {if(level.gateCondition.isMet(collectedClues,levelGems)&&gatePuzzleSolved){gateUnlocked=true;nearby.active=false;level.solids.remove(new Rect(nearby.bounds.x(),nearby.bounds.y(),nearby.bounds.w(),nearby.bounds.h()));openDialogue("THE ANCIENT GATE", "The Wave and Thorn marks flare. Gears grind beneath the jungle and the gate sinks into the earth. The final trail is open.");journal("The ancient gate opened when the required marks were presented.");}else openDialogue("SEALED GATE", "The gate needs Clues B and C plus the solved stone dial. Enter THORN then SUN at the dial nearby.");}
            default -> { }
        }
    }
    private void talkTo(Item npc){
        String title=npc.label;String text;
        if(npc.id.equals("L1-NPC"))text="MARA, SHIPWRECK SURVIVOR\n\n'Captain Elias Vale brought our crew here chasing the Lost Vault. The storm was no accident: he found a warning carved into the lighthouse and sailed anyway. I saw him leave three gem markers along the shore before the tide swallowed our ship. Search the crossing paths, not just the obvious trail. If you find a scroll marked with a wave, keep it safe. It may be the first piece of a much older map.'\n\nNEW LEAD: Find Clue A and follow the shore's branching trails.";
        else if(npc.id.equals("L2-NPC"))text="THE LOST CARTOGRAPHER\n\n'I followed Vale inland. The jungle paths form a grid because the builders wanted explorers to choose: the safe route is longer, the shortcut is trapped, and the clues sit on opposite branches. The gate listens for two marks—Wave and Thorn. I found one near the ruined shrine. The other is beyond the breathing sand. Bring both, and the gate should release the final route.'\n\nFIELD TIP: Check side branches for gems and inscriptions. Quicksand costs a life.";
        else text="THE KEEPER\n\n'You have carried the Wave, Thorn, and Sun. Good. The Vault was never a king's tomb. Vale learned the truth too late: sailors hid the crown here so a tyrant could never use it to buy an empire. The rescue boat was left for the one who solved the map—not the one who grabbed gold. When the chest opens, the island will begin to break. Take the treasure and run the grid trail east to the harbor. No hero survives by standing still.'\n\nFINAL OBJECTIVE: Open the Vault, claim the treasure, then interact with the rescue boat.";
        dialogueContext=npc.id;dialogueChoices=npc.id.equals("L1-NPC")?new String[]{"Ask about the storm", "Ask for useful supplies", "Ask about Captain Vale"}:npc.id.equals("L2-NPC")?new String[]{"Ask for the safe route", "Ask about the stone dial", "Ask about hidden chambers"}:new String[]{"Ask about the crown", "Promise to return the seal", "Ask about the rescue boat"};dialogueChoice=0;openDialogue(title,text);journal("Conversation — "+title+": "+text);
    }
    public void chooseDialogueOption(int option){if(!dialogueOpen||option<0||option>=dialogueChoices.length)return;String choice=dialogueChoices[option];dialogueChoice=option;switch(dialogueContext){case "L1-NPC"->{if(option==1){inventory.add("Rusty key");message("Mara gives you a rusty key. A vine-covered wall may hide a cache.");journal("Mara's supply: Rusty key. She says smugglers hid tools near the shore camp.");}else if(option==0){journal("Mara confirms the storm was summoned to hide the crown, not a natural disaster.");}else journal("Mara says Vale regretted leaving a crew member behind. The crown may test more than greed.");}case "L2-NPC"->{if(option==1)journal("Cartographer's puzzle hint: Thorn first, then Sun (keys 2, 3). The dial opens only when both clues are understood.");else if(option==2)journal("Cartographer's secret hint: inspect cracked walls after collecting the Thorn clue.");else journal("Cartographer's route hint: the northern loop avoids quicksand but the southern shortcut hides a gem.");}case "L3-NPC"->{if(option==1){inventory.add("Keeper's Promise");journal("You promised the Keeper to return the seal rather than sell it. The ending may change.");}else if(option==0)journal("The Keeper confirms the crown belongs to the people who resisted the tyrant.");else journal("The rescue boat lies beyond the eastern harbor markers; the island will collapse after the crown is lifted.");}default->{}}dialogueText="CHOICE RECORDED: "+choice+"\n\n"+"Your decision has been added to the expedition journal. You can keep asking questions or press Enter to return to exploration.";dialogueChoices=new String[0];dialogueContext="";}
    private void inspectInscription(Item item){
        String text=item.id.equals("L1-I1")?"'When the tide retreats, follow the green path. Three bright stones mark the route; the first clue rests where the ship's shadow points.'":item.id.equals("L2-I1")?"'Wave opens the way. Thorn guards the way. Sun remembers the way home.'":"'The vault opens for the three marks. The boat waits only for those who return with the truth.'";
        openDialogue("ANCIENT INSCRIPTION",text);journal("Inscription: "+text);
    }
    public void togglePause(){if(dialogueOpen){closeDialogue();return;}if(journalOpen){journalOpen=false;return;}if(inventoryOpen){inventoryOpen=false;return;}if(mapOpen){mapOpen=false;return;}if(puzzleOpen){cancelPuzzle();return;}if(state==State.PLAYING)state=State.PAUSED;else if(state==State.PAUSED)state=State.PLAYING;}
    public void openHowTo(){stateBeforeHowTo=state;state=State.HOW_TO_PLAY;}
    public void closeHowTo(){state=stateBeforeHowTo;}
    public void resume(){if(state==State.PAUSED)state=State.PLAYING;}
    public void restartLevel(){runClues.remove(levelNumber);startLevel(levelNumber);state=State.PLAYING;}
    public void returnToMenu(){state=State.MENU;level=null;dialogueOpen=false;journalOpen=false;inventoryOpen=false;mapOpen=false;puzzleOpen=false;}
    public void continueAfterTransition(){if(state!=State.TRANSITION)return;if(levelNumber<3){startLevel(levelNumber+1);state=State.PLAYING;}else state=State.VICTORY;}
    public void message(String text){message=text;messageTimer=2.4;}
    public boolean isNearActive(Kind kind){if(level==null)return false;Rect near=new Rect(playerX-34,playerY-34,PLAYER_SIZE+68,PLAYER_SIZE+68);return level.items.stream().anyMatch(i->i.active&&i.kind==kind&&near.intersects(i.bounds));}
    public boolean isNearInteractable(){return isNearActive(Kind.CLUE)||isNearActive(Kind.VAULT)||isNearActive(Kind.BOAT)||isNearActive(Kind.GATE)||isNearActive(Kind.NPC)||isNearActive(Kind.INSCRIPTION)||isNearActive(Kind.TREASURE)||isNearActive(Kind.KEY)||isNearActive(Kind.PUZZLE)||isNearActive(Kind.SECRET)||isNearActive(Kind.CACHE)||isNearActive(Kind.SWITCH);}
    public boolean hasClue(String clue){return collectedClues.contains(clue);}

    // Package-private deterministic hooks used only by the headless regression suite.
    void teleportForTest(double x,double y){playerX=x;playerY=y;}
    void setCheckpointForTest(double x,double y){checkpointX=x;checkpointY=y;checkpoint=1;}
    void forceLivesForTest(int value){lives=value;invulnerable=0;}
    void debugStartLevel(int n){highestUnlocked=3;startLevel(n);state=State.PLAYING;}
    void debugSetClues(Set<String> clues){collectedClues.clear();collectedClues.addAll(clues);runClues.put(levelNumber,new LinkedHashSet<>(clues));}
    void debugSetTreasure(boolean value){hasTreasure=value;}
    void debugSetEscapeTimer(double value){escapeTimer=value;}
    void debugSolveGatePuzzle(){gatePuzzleSolved=true;}
    void debugSolveVaultPuzzle(){vaultPuzzleSolved=true;}
}

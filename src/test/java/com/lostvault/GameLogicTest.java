package com.lostvault;

import java.util.Set;

/** Dependency-free executable regression suite for core rules. */
public final class GameLogicTest {
    private static int passed=0;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError("FAILED: "+name);passed++;System.out.println("PASS: "+name);}
    private static void collectAllGemsAndClues(GameModel g){
        for(GameModel.Item item:g.level().items.stream().filter(i->i.kind==GameModel.Kind.GEM).toList()){g.teleportForTest(item.bounds.x(),item.bounds.y());g.update(0,0,0,false);}
        for(GameModel.Item item:g.level().items.stream().filter(i->i.kind==GameModel.Kind.CLUE).toList()){g.teleportForTest(item.bounds.x(),item.bounds.y());g.update(0,0,0,false);}
    }
    public static void main(String[] args){
        double[] d=GameModel.normalizedMovement(1,1);check(Math.abs(Math.hypot(d[0],d[1])-1)<1e-9,"diagonal movement is normalized");
        double[] c=GameModel.normalizedMovement(1,0);check(Math.abs(c[0]-1)<1e-9&&c[1]==0,"cardinal movement remains unchanged");
        check(new Rect(0,0,10,10).intersects(new Rect(9,9,3,3)),"AABB detects overlap");check(!new Rect(0,0,10,10).intersects(new Rect(10,0,3,3)),"AABB touching edges is not overlap");
        UnlockCondition cond=new UnlockCondition(Set.of("B","C"),5);check(cond.isMet(Set.of("B","C"),5),"unlock accepts all clues and gem threshold");check(!cond.isMet(Set.of("B"),9),"unlock rejects missing clue");check(!cond.isMet(Set.of("B","C"),4),"unlock rejects insufficient gems");
        GameModel g=new GameModel();g.startNewRun();check(g.state()==GameModel.State.PLAYING&&g.levelNumber()==1,"new run starts level 1");check(g.lives()==3,"level starts with three lives");
        GameModel.Level l=g.level();GameModel.Item gem=l.items.stream().filter(i->i.kind==GameModel.Kind.GEM).findFirst().orElseThrow();double gx=gem.bounds.x(),gy=gem.bounds.y();g.update(0,0,0,false);g.teleportForTest(gx,gy);g.update(0,0,0,false);check(g.levelGems()==1&&g.totalGems()==1&&!gem.active,"gem collected once");g.teleportForTest(gx,gy);g.update(0,0,0,false);check(g.levelGems()==1&&g.totalGems()==1,"duplicate gem collection is a no-op");
        GameModel.Item clue=l.items.stream().filter(i->i.kind==GameModel.Kind.CLUE).findFirst().orElseThrow();g.teleportForTest(clue.bounds.x(),clue.bounds.y());g.update(0,0,0,false);check(g.hasClue("A"),"clue id is recorded");g.teleportForTest(clue.bounds.x(),clue.bounds.y());g.update(0,0,0,false);check(g.collectedClues().size()==1,"duplicate clue collection is a no-op");
        g.setCheckpointForTest(450,700);g.teleportForTest(720,470);g.update(.016,0,0,false);check(g.lives()==2&&g.playerX()==450&&g.playerY()==700,"hazard costs a life and respawns at checkpoint");
        g.forceLivesForTest(1);g.teleportForTest(720,470);g.update(.016,0,0,false);check(g.levelNumber()==1&&g.lives()==3&&g.levelGems()==0,"last life restarts current level attempt");
        GameModel gate=new GameModel();gate.startNewRun();check(gate.levelNumber()==1,"level progression begins in order");boolean rejected=false;try{gate.startLevel(2);}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"locked level cannot be entered directly");
        GameModel vault=new GameModel();vault.startNewRun();vault.teleportForTest(200,200);vault.debugStartLevel(3);check(!vault.vaultUnlocked()&&!vault.hasTreasure(),"vault begins locked and treasure absent");vault.debugSetClues(Set.of("A","B","C"));vault.teleportForTest(1285,345);vault.update(0,0,0,true);check(vault.vaultUnlocked()&&vault.treasureSpawned(),"vault unlock reveals treasure exactly once");vault.update(0,0,0,true);check(vault.level().items.stream().filter(i->i.kind==GameModel.Kind.TREASURE).count()==1,"repeated interaction does not duplicate treasure");
        GameModel boat=new GameModel();boat.startNewRun();boat.debugStartLevel(3);boat.teleportForTest(1590,940);boat.update(0,0,0,true);check(boat.state()!=GameModel.State.VICTORY,"boat cannot trigger victory without treasure");boat.debugSetTreasure(true);boat.teleportForTest(1590,940);boat.update(0,0,0,true);check(boat.state()==GameModel.State.VICTORY,"boat triggers victory after treasure when interacted");
        GameModel pause=new GameModel();pause.startNewRun();pause.togglePause();double x=pause.playerX();pause.update(1,1,0,false);check(pause.state()==GameModel.State.PAUSED&&pause.playerX()==x,"paused state freezes simulation");pause.resume();check(pause.state()==GameModel.State.PLAYING,"resume returns to gameplay");
        GameModel full=new GameModel();full.startNewRun();
        check(full.level().items.stream().filter(i->i.kind==GameModel.Kind.GEM).count()>=3&&full.level().items.stream().filter(i->i.kind==GameModel.Kind.CHECKPOINT).count()>=2,"Level 1 data has enough gems and checkpoints");
        collectAllGemsAndClues(full);full.teleportForTest(1645,1035);full.update(0,0,0,false);check(full.state()==GameModel.State.TRANSITION,"Level 1 completion transitions only after objectives");full.continueAfterTransition();check(full.levelNumber()==2&&full.state()==GameModel.State.PLAYING,"transition unlocks Level 2 in order");
        check(full.level().items.stream().filter(i->i.kind==GameModel.Kind.GEM).count()>=5&&full.level().items.stream().filter(i->i.kind==GameModel.Kind.CHECKPOINT).count()>=2&&full.level().items.stream().anyMatch(i->i.kind==GameModel.Kind.HAZARD),"Level 2 data has enough gems, checkpoints and hazards");
        collectAllGemsAndClues(full);full.teleportForTest(1510,780);full.update(0,0,0,true);check(full.gateUnlocked(),"Level 2 gate unlocks after both clues");full.teleportForTest(1665,1035);full.update(0,0,0,false);check(full.state()==GameModel.State.TRANSITION,"Level 2 exit requires the gate and objectives");full.continueAfterTransition();check(full.levelNumber()==3,"Level 3 follows Level 2 without skipping");
        check(full.level().items.stream().filter(i->i.kind==GameModel.Kind.GEM).count()>=4&&full.level().items.stream().filter(i->i.kind==GameModel.Kind.CHECKPOINT).count()>=2,"Level 3 data has enough gems and checkpoints");
        collectAllGemsAndClues(full);full.teleportForTest(1285,345);full.update(0,0,0,true);check(full.vaultUnlocked()&&full.treasureSpawned(),"full run unlocks Vault with carried clues");full.teleportForTest(1320,425);full.update(0,0,0,false);check(full.hasTreasure(),"treasure pickup works in the full run");full.teleportForTest(1590,940);full.update(0,0,0,true);check(full.state()==GameModel.State.VICTORY,"full game progression reaches Victory");
        System.out.println("\nAll "+passed+" checks passed.");
    }
}

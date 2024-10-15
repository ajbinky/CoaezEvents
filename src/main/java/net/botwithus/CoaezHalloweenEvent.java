package net.botwithus;

import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.movement.TraverseEvent;
import net.botwithus.rs3.game.queries.builders.animations.SpotAnimationQuery;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.animation.SpotAnimation;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.inventories.Equipment;

import java.util.*;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class CoaezHalloweenEvent extends LoopingScript {

    public BotState botState = BotState.IDLE;
    public BotState lastActivityState = BotState.IDLE;
    public final Random random = new Random();
    private ScriptConfig config;
    public final CoaezHalloweenEventGraphicsContext sgc;

    private final Coordinate bankLocation = new Coordinate(605, 1701, 0);
    private final Coordinate excavationLocation = new Coordinate(574, 1738, 0);
    private final Area bankArea = new Area.Circular(bankLocation, 10);
    private final Area excavationArea = new Area.Circular(excavationLocation, 15);
    private final Coordinate summoningLocation = new Coordinate(586, 1742, 0);
    private final Area summoningArea = new Area.Circular(summoningLocation, 15);

    public enum BotState {
        SUMMONING,
        ARCHAEOLOGY,
        THIEVING,
        PUMPKIN,
        TURNINCOLLECTIONS,
        MAZE,
        IDLE
    }

    public boolean destroyArchBook;
    public boolean identifyAncientRemains;
    private long lastAnimationChangeTime = 0;
    private int lastAnimationId = -1;
    public boolean chaseSprite;
    public int ancientRemainsCount = 27;
    public int thievingDelay = 10;
    public int interactionCount = 0;
    public int maxInteractionsBeforePause = random.nextInt(26) + 15;
    public int minWaitTime = 10;
    public int maxWaitTime = 60;

//    private final Coordinate mazeStartLocation = new Coordinate(624,1715,0);
//    private final Area mazeEntranceArea = new Area.Circular(mazeStartLocation, 5);
//    boolean usedDoor = false;
//
//    private final Area bossAreaZone1 = new Area.Rectangular(new Coordinate(699, 1729, 0), new Coordinate(700, 1750, 0));
//    private final Area bossAreaZone2 = new Area.Rectangular(new Coordinate(699, 1749, 0), new Coordinate(717, 1750, 0));
//    private final Area bossAreaZone3 = new Area.Rectangular(new Coordinate(699, 1729, 0), new Coordinate(720, 1731, 0));
//    private final Area bossAreaZone4 = new Area.Rectangular(new Coordinate(718, 1729, 0), new Coordinate(720, 1749, 0));
//    private final Area gateSearchArea = new Area.Rectangular(new Coordinate(697, 1728, 0), new Coordinate(722, 1752, 0));
//    private final int[][] directions = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} }; // Up, Right, Down, Left
//    private final Coordinate mazeStart = new Coordinate(660, 1690, 0);
//    private final Coordinate mazeEnd = new Coordinate(759, 1789, 0);

    enum TransferOptionType {
        ONE(2, 33882205),
        FIVE(3, 33882208),
        TEN(4, 33882211),
        ALL(7, 33882215),
        X(5, 33882218);

        private final int varbitValue;
        private final int componentValue;

        TransferOptionType(int varbitValue, int componentValue) {
            this.varbitValue = varbitValue;
            this.componentValue = componentValue;
        }

        public int getComponentValue() {
            return this.componentValue;
        }

        public int getVarbitValue() {
            return this.varbitValue;
        }
    }
    public CoaezHalloweenEvent(String s, ScriptConfig config, ScriptDefinition scriptDefinition) {
        super(s, config, scriptDefinition);
        this.config = config;
        this.sgc = new CoaezHalloweenEventGraphicsContext(this.getConsole(), this);

    }

    @Override
    public void onLoop() {
        Player player = getLocalPlayer();

        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN) {
            Execution.delay(random.nextLong(2500, 5500));
            return;
        }

        sgc.saveConfig();
        switch (botState) {
            case TURNINCOLLECTIONS:
                handleCollectionTurnIn(player);
                break;
            case PUMPKIN:
                handlePumpkin(player);
                break;
            case THIEVING:
                handleThieving(player);
                break;
            case SUMMONING:
                handleSummoning(player);
                break;
            case ARCHAEOLOGY:
                handleArchaeology(player);
                break;
            default:
                Execution.delay(random.nextLong(1000, 3000));
                break;
        }
    }

    private void handleCollectionTurnIn(Player player) {
        if (!Bank.isOpen()) {
            println("Opening the bank...");
            Bank.open();
            Execution.delayUntil(5000, Bank::isOpen);
            Execution.delay(random.nextLong(600, 1200));
            if (!Bank.isOpen()) {
                println("Failed to open the bank.");
                return;
            }
        }

        String[] firstCollectionItems = {
                "Soiled Storybook", "Polished Horn", "Dusty Snow Globe",
                "Ruined Letter", "Strange Coins"
        };

        String[] secondCollectionItems = {
                "Ancient Skull", "Ancient Torso", "Ancient Left Arm",
                "Ancient Right Arm", "Ancient Left Leg", "Ancient Right Leg"
        };

        int firstCollectionItemCount = 5;
        int secondCollectionItemCount = 6;

        int firstCollectionAvailable = calculateAvailableCollections(firstCollectionItems, firstCollectionItemCount);
        int secondCollectionAvailable = calculateAvailableCollections(secondCollectionItems, secondCollectionItemCount);

        println("You have enough items to complete " + firstCollectionAvailable + " first collections.");
        println("You have enough items to complete " + secondCollectionAvailable + " second collections.");

        int maxInventoryForFirst = Math.min(28 / firstCollectionItemCount, firstCollectionAvailable);
        int maxInventoryForSecond = Math.min(28 / secondCollectionItemCount, secondCollectionAvailable);

        if (maxInventoryForFirst > 0 && withdrawCollectionItems(firstCollectionItems, maxInventoryForFirst)) {
            Bank.close();
            println("First collection items successfully withdrawn. Turning them in.");
            handleCollectionSubmission();
        }

        if (maxInventoryForSecond > 0 && withdrawCollectionItems(secondCollectionItems, maxInventoryForSecond)) {
            Bank.close();
            println("Second collection items successfully withdrawn. Turning them in.");
            handleSecondCollectionSubmission();
        }
    }

    private int calculateAvailableCollections(String[] collectionItems, int collectionItemCount) {
        int minItemCount = Integer.MAX_VALUE;

        for (String item : collectionItems) {
            if (Bank.contains(item)) {
                int itemCount = Bank.getCount(item);
                int possibleCollections = itemCount / collectionItemCount;
                println("Found " + itemCount + " of " + item + " in the bank. Can make " + possibleCollections + " collections from it.");
                minItemCount = Math.min(minItemCount, possibleCollections);
            } else {
                println("Item " + item + " not found in the bank.");
                return 0;
            }
        }

        return minItemCount;
    }

    private boolean withdrawCollectionItems(String[] collectionItems, int maxCollections) {
        for (String item : collectionItems) {
            if (Bank.contains(item)) {
                for (int i = 0; i < maxCollections; i++) {
                    if (!Bank.withdraw(item, TransferOptionType.ONE.getVarbitValue())) {
                        println("Failed to withdraw " + item + " on attempt " + (i + 1));
                        return false;
                    }
                    Execution.delay(random.nextLong(100, 200));
                }
            } else {
                println(item + " not found in the bank.");
                return false;
            }
        }
        return true;
    }


    private void handlePumpkin(Player player) {
        println("Handling pumpkin interaction. Interaction count: " + interactionCount + " / " + maxInteractionsBeforePause);

        EntityResultSet<Npc> pumpkinResults = NpcQuery.newQuery()
                .name("Smashing Pumpkin")
                .option("Smash")
                .results();
        Npc pumpkin = pumpkinResults.nearest();

        if (pumpkin != null) {
            println("Smashing pumpkin...");
            pumpkin.interact("Smash");
            Execution.delay(random.nextLong(2300, 2400));

            interactionCount++;

            println("Interaction #" + interactionCount + " completed.");

            if (interactionCount >= maxInteractionsBeforePause) {
                long randomWait = random.nextLong(minWaitTime * 1000, maxWaitTime * 1000);
                println("Pausing for " + randomWait / 1000 + " seconds after " + interactionCount + " interactions.");

                Execution.delay(randomWait);

                interactionCount = 0;
                maxInteractionsBeforePause = random.nextInt(26) + 15;

                println("Next pause will be after " + maxInteractionsBeforePause + " interactions.");
            }
        } else {
            println("No Smashing Pumpkin found.");
        }
    }

    private void handleThieving(Player player) {

        if(Backpack.contains("Ancient remains", ancientRemainsCount) && identifyAncientRemains && !Backpack.isFull()){
            Backpack.interact("Ancient remains", "Identify all");
            Execution.delay(random.nextLong(1200, 1800));
        }

        if (backpackContainsSecondCollectionItems()) {
            handleSecondCollectionSubmission();
            return;
        }

        if (Backpack.isFull()) {
            println("Backpack is full. Moving to bank to deposit.");
            if (!bankArea.contains(player.getCoordinate())) {
                println("Player is not in the bank area, moving to bank...");
                Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
                Execution.delay(random.nextLong(6000, 10000));
                return;
            }
            if (bankArea.contains(player.getCoordinate())) {
                println("Player is in the bank area, opening the bank...");
                Bank.open();
                Execution.delayUntil(10000, Bank::isOpen);
                if(Bank.isOpen()){
                    Execution.delay(random.nextLong(1000, 2000));
                    Bank.depositAllExcept("Complete tome");
                    Bank.close();
                    return;
                }
            }
            return;
        }

        int currentAnimationId = player.getAnimationId();

        if (currentAnimationId != lastAnimationId) {
            lastAnimationId = currentAnimationId;
            lastAnimationChangeTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - lastAnimationChangeTime >= (thievingDelay * 1000)) {
            if (currentAnimationId == -1) {
                EntityResultSet<SceneObject> lootableObjects = SceneObjectQuery.newQuery()
                        .option("Loot")
                        .results();
                SceneObject nearestLootable = lootableObjects.nearest();

                if (nearestLootable != null) {
                    println("Lootable object found. Interacting...");
                    nearestLootable.interact("Loot");
                    Execution.delay(random.nextLong(2000, 4000));
                } else {
                    println("No lootable objects nearby.");
                }
            } else {
                if (currentAnimationId == 24887) {
                    println("No loot animation detected for " + (thievingDelay) + " seconds. Searching for new loot...");
                    EntityResultSet<SceneObject> lootableObjects = SceneObjectQuery.newQuery()
                            .option("Loot")
                            .results();
                    SceneObject nearestLootable = lootableObjects.nearest();
                    if (nearestLootable != null) {
                        println("New lootable object found. Interacting...");
                        nearestLootable.interact("Loot");
                        Execution.delay(random.nextLong(6000, 8000));
                    } else {
                        println("No other lootable objects found.");
                    }
                } else {
                    println("Player is already interacting. Waiting...");
                    Execution.delay(random.nextLong(1000, 2000));
                }
            }
        } else {
            Execution.delay(random.nextLong(600, 1200));
        }
    }



    private void handleSummoning(Player player) {

        if(!summoningArea.contains(player)){
            println("Moving to summoning area");
            moveTo(summoningArea.getRandomWalkableCoordinate());
        }

        if(Backpack.contains("Ancient remains", ancientRemainsCount) && identifyAncientRemains){
            Backpack.interact("Ancient remains", "Identify all");
            Execution.delay(random.nextLong(1200, 1800));
        }

        if (backpackContainsSecondCollectionItems()) {
            handleSecondCollectionSubmission();
            return;
        }

        if (Backpack.isFull()) {
            println("Backpack is full. Moving to bank to load the last preset.");
            if (!bankArea.contains(player.getCoordinate())) {
                println("Player is not in the bank area, moving to bank...");
                Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
                Execution.delay(random.nextLong(6000, 10000));
                return;
            }
            if (bankArea.contains(player.getCoordinate())) {
                println("Player is in the bank area, opening the bank...");
                Bank.open();
                Execution.delayUntil(10000, Bank::isOpen);
                if(Bank.isOpen()){
                    Execution.delay(random.nextLong(1000, 2000));
                    Bank.depositAllExcept("Complete tome");
                    Bank.close();
                    return;
                }
            }
            return;
        }

        EntityResultSet<SceneObject> unlitCandleResults = SceneObjectQuery.newQuery()
                .name("Candle")
                .option("Light")
                .results();
        SceneObject unlitCandle = unlitCandleResults.nearest();

        if (unlitCandle != null) {
            println("Unlit candle found. Lighting...");
            unlitCandle.interact("Light");
            Execution.delay(random.nextLong(2000, 4000));
            return;
        }

        EntityResultSet<SceneObject> summoningCircleResults = SceneObjectQuery.newQuery()
                .name("Summoning circle")
                .option("Summon")
                .results();
        SceneObject summoningCircle = summoningCircleResults.nearest();

        if (summoningCircle != null) {
            if (player.getAnimationId() == 35520) {
                unlitCandle = unlitCandleResults.nearest();
                if (unlitCandle != null) {
                    println("Lighting candle during ritual...");
                    unlitCandle.interact("Light");
                    Execution.delay(random.nextLong(2000, 4000));
                    summoningCircle.interact("Summon");
                    Execution.delay(random.nextLong(3000, 5000));
                    return;
                }
            }

            if(player.getAnimationId() != 35520) {
                println("Starting summoning ritual...");
                summoningCircle.interact("Summon");
                Execution.delayUntil(2000, () -> player.getAnimationId() == 35520);
            }
        }
    }

    private void handleArchaeology(Player player) {
        if (Interfaces.isOpen(1189)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77922323);
        }

        if (Backpack.contains("Ancient remains", ancientRemainsCount) && identifyAncientRemains) {
            Backpack.interact("Ancient remains", "Identify all");
            Execution.delay(random.nextLong(1200, 1800));
        }

        if (backpackContainsCollectionItems()) {
            handleCollectionSubmission();
            return;
        }

        if (backpackContainsSecondCollectionItems()) {
            handleSecondCollectionSubmission();
            return;
        }

        if (Backpack.isFull()) {
            println("Backpack is full. Moving to bank to load the last preset.");
            if (!bankArea.contains(player.getCoordinate())) {
                println("Player is not in the bank area, moving to bank...");
                Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
                Execution.delay(random.nextLong(6000, 10000));
                return;
            }
            if (bankArea.contains(player.getCoordinate())) {
                println("Player is in the bank area, opening the bank...");
                Bank.open();
                Execution.delayUntil(10000, Bank::isOpen);
                if (Bank.isOpen()) {
                    Execution.delay(random.nextLong(1000, 2000));
                    Bank.depositAllExcept("Complete tome");
                    Bank.close();
                    return;
                }
            }
            return;
        }

        if (!excavationArea.contains(player.getCoordinate())) {
            println("Returning to excavation site at " + excavationLocation);
            Movement.walkTo(excavationLocation.getX(), excavationLocation.getY(), true);
            Execution.delay(random.nextLong(3000, 5000));
            return;
        }

        if (chaseSprite) {
            EntityResultSet<SpotAnimation> spotResults = SpotAnimationQuery.newQuery()
                    .animations(7307)
                    .results();
            SpotAnimation archaeologySpot = spotResults.nearest();

            if (archaeologySpot != null) {
                Coordinate spotCoordinate = archaeologySpot.getCoordinate();
                println("SpotAnimation found at: " + spotCoordinate);

                double distanceToSpot = player.getCoordinate().distanceTo(spotCoordinate);

                if (player.getAnimationId() == 33350 && distanceToSpot <= 1) {
                    println("Player is already excavating. Spot is within 1 tile.");
                    Execution.delay(random.nextLong(2000, 4000));
                    return;
                }

                if (player.getAnimationId() == -1 || distanceToSpot > 1) {
                    println("Interacting with 'Mystery remains' at: " + spotCoordinate);

                    SceneObject mysteryRemains = SceneObjectQuery.newQuery()
                            .name("Mystery remains")
                            .option("Excavate")
                            .on(spotCoordinate)
                            .results()
                            .nearest();

                    if (mysteryRemains != null) {
                        println("Interacting with 'Mystery remains' at: " + spotCoordinate);
                        mysteryRemains.interact("Excavate");
                        Execution.delay(random.nextLong(3000, 5000));
                    } else {
                        println("No 'Mystery remains' found at the spot animation location.");
                    }
                }
            } else {
                println("No archaeology SpotAnimation found nearby. Interacting with 'Ancient remains'.");
                SceneObject mysteryRemains = SceneObjectQuery.newQuery()
                        .name("Mystery remains")
                        .option("Excavate")
                        .results()
                        .nearest();


                if (mysteryRemains != null) {
                    println("Interacting with 'Ancient remains' to reveal hidden remains.");
                    mysteryRemains.interact("Excavate");
                    Execution.delay(random.nextLong(3000, 5000));
                } else {
                    println("No 'Ancient remains' found to interact with.");
                }
            }
        } else {
            if (player.getAnimationId() == -1) {
                println("Chase sprite is disabled, interacting with the nearest 'Mystery remains'.");
                SceneObject mysteryRemains = SceneObjectQuery.newQuery()
                        .name("Mystery remains")
                        .option("Excavate")
                        .results()
                        .nearest();

                if (mysteryRemains != null) {
                    println("Interacting with 'Mystery remains'.");
                    mysteryRemains.interact("Excavate");
                    Execution.delay(random.nextLong(3000, 5000));
                } else {
                    println("No 'Mystery remains' found to interact with.");
                }
            } else {
                Execution.delay(random.nextLong(3000, 5000));
            }
        }
    }


    private boolean backpackContainsCollectionItems() {
        String[] collectionItems = {
                "Soiled storybook", "Polished horn", "Dusty snow globe", "Ruined letter", "Strange coins"
        };

        for (String item : collectionItems) {
            if (!Backpack.contains(item)) {
                return false;
            }
        }
        return true;
    }

    private boolean backpackContainsSecondCollectionItems() {
        String[] secondCollectionItems = {
                "Ancient skull", "Ancient torso", "Ancient left arm", "Ancient right arm", "Ancient left leg", "Ancient right leg"
        };

        for (String item : secondCollectionItems) {
            if (!Backpack.contains(item)) {
                return false;
            }
        }
        return true;
    }

    private void handleSecondCollectionSubmission() {
        println("Found second collection items in backpack. Finding NPC 'Eep'...");

        EntityResultSet<Npc> eepResults = NpcQuery.newQuery()
                .name("Eep")
                .option("Talk to")
                .results();
        Npc eep = eepResults.nearest();

        if (eep != null) {
            println("Interacting with NPC 'Eep'...");
            int attempts = 0;
            boolean interactionSuccess = false;

            while (attempts < 3 && !interactionSuccess) {
                interactionSuccess = eep.interact("Collections");
                if (!interactionSuccess) {
                    println("Failed to interact with NPC, retrying... (" + (attempts + 1) + "/3)");
                    attempts++;
                    Execution.delay(random.nextLong(1000, 2000));
                }
            }

            if (interactionSuccess) {
                println("Opening collections window...");
                if (Execution.delayUntil(15000, () -> Interfaces.isOpen(656))) {
                    println("Interface 656 opened, switching to second collection...");

                    Execution.delay(random.nextLong(600, 1200));

                    ResultSet<Component> secondCollectionTab = ComponentQuery.newQuery(656)
                            .componentIndex(30)
                            .results();
                    Component secondTab = secondCollectionTab.first();
                    if (secondTab != null && !secondTab.isHidden()) {
                        println("Second collection tab is visible. Attempting to switch...");
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 1, 42991647);
                        Execution.delay(random.nextLong(1200, 1800));

                        println("Confirming second collection contribution...");

                        ResultSet<Component> contributeAllComponent = ComponentQuery.newQuery(656)
                                .componentIndex(24)
                                .results();
                        Component confirmButton = contributeAllComponent.first();
                        if (confirmButton != null && !confirmButton.isHidden()) {
                            println("'Contribute All' button is visible. Attempting to confirm...");
                            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);
                            Execution.delay(random.nextLong(1200, 1800));

                            println("Second collection submitted. Returning to excavation.");
                        } else {
                            println("Failed to find or interact with 'Contribute All' button.");
                        }
                    } else {
                        println("Failed to find or interact with second collection tab.");
                    }
                } else {
                    println("Failed to open interface 656.");
                }
            } else {
                println("NPC interaction failed after 3 attempts.");
            }
        } else {
            println("NPC 'Eep' not found.");
        }
    }

    private void handleCollectionSubmission() {
        println("Found collection items in backpack. Finding NPC 'Eep'...");

        EntityResultSet<Npc> eepResults = NpcQuery.newQuery()
                .name("Eep")
                .option("Talk to")
                .results();
        Npc eep = eepResults.nearest();

        if (eep != null) {
            println("Interacting with NPC 'Eep'...");
            int attempts = 0;
            boolean interactionSuccess = false;

            while (attempts < 3 && !interactionSuccess) {
                interactionSuccess = eep.interact("Collections");
                if (!interactionSuccess) {
                    println("Failed to interact with NPC, retrying... (" + (attempts + 1) + "/3)");
                    attempts++;
                    Execution.delay(random.nextLong(1000, 2000));
                }
            }

            if (interactionSuccess) {
                println("Opening collections window...");
                if (Execution.delayUntil(10000, () -> Interfaces.isOpen(656))) {
                    println("Interface 656 opened, confirming collection submission...");

                    ResultSet<Component> contributeAllComponent = ComponentQuery.newQuery(656)
                            .componentIndex(24)
                            .results();

                    Component confirmButton = contributeAllComponent.first();
                    if (confirmButton != null && !confirmButton.isHidden()) {
                        println("Found 'Contribute All' button. Attempting to interact...");
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);
                        Execution.delay(random.nextLong(1200, 1800));

                        println("Collection submitted. Returning to excavation.");
                    } else {
                        println("Failed to find or interact with 'Contribute All' button.");
                    }
                } else {
                    println("Failed to open interface 656.");
                }
            } else {
                println("NPC interaction failed after 3 attempts.");
            }
        } else {
            println("NPC 'Eep' not found.");
        }
    }



    public boolean moveTo(Coordinate location) {
        Player player = getLocalPlayer();
        if (location.distanceTo(player.getCoordinate()) < 1) return true;
        NavPath path = NavPath.resolve(location);
        TraverseEvent.State moveState = Movement.traverse(path);
        return moveState == TraverseEvent.State.FINISHED;
    }

    public void setBotState(BotState state) {
        this.botState = state;
        println("Bot state changed to: " + state);
    }
    public ScriptConfig getConfig() {
        return config;
    }

    @Override
    public CoaezHalloweenEventGraphicsContext getGraphicsContext() {
        return sgc;
    }
}

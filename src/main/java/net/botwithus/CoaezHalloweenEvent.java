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
import java.util.Random;

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
        IDLE
    }

    public boolean destroyArchBook;
    public boolean identifyAncientRemains;
    private long lastAnimationChangeTime = 0;
    private int lastAnimationId = -1;
    public boolean chaseSprite;

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

    private void handlePumpkin(Player player) {
        EntityResultSet<Npc> pumpkinResults = NpcQuery.newQuery()
                .name("Smashing Pumpkin")
                .option("Smash")
                .results();
        Npc pumpkin = pumpkinResults.nearest();

        if (pumpkin != null) {
            pumpkin.interact("Smash");
            Execution.delay(2250);
        }
    }

    private void handleThieving(Player player) {
        int currentAnimationId = player.getAnimationId();

        if (currentAnimationId != lastAnimationId) {
            lastAnimationId = currentAnimationId;
            lastAnimationChangeTime = System.currentTimeMillis();
        }

        // Check if the animation has stayed the same for at least 5 seconds
        if (System.currentTimeMillis() - lastAnimationChangeTime >= 5000) {
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
                // Handle player 24887 case (already looting, but no animation after 5 seconds)
                if (player.getId() == 24887) {
                    println("No loot animation detected for 5 seconds. Searching for new loot...");
                    EntityResultSet<SceneObject> lootableObjects = SceneObjectQuery.newQuery()
                            .option("Loot")
                            .results();
                    SceneObject nearestLootable = lootableObjects.nearest();
                    if (nearestLootable != null) {
                        println("New lootable object found. Interacting...");
                        nearestLootable.interact("Loot");
                        Execution.delay(random.nextLong(2000, 4000));
                    } else {
                        println("No other lootable objects found.");
                    }
                } else {
                    println("Player is already interacting. Waiting...");
                    Execution.delay(random.nextLong(1000, 3000));
                }
            }
        } else {
            println("Waiting for the animation...");
            Execution.delay(random.nextLong(100, 200));
        }
    }



    private void handleSummoning(Player player) {

        if(!summoningArea.contains(player)){
            println("Moving to summoning area");
            moveTo(summoningArea.getRandomWalkableCoordinate());
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
        println("Handling Archaeology...");

        if (Interfaces.isOpen(1189)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77922323);
        }

        if(Backpack.contains("Ancient remains", 27) && identifyAncientRemains){
            Backpack.interact("Ancient remains", "Identify all");
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
                Execution.delay(random.nextLong(3000, 5000));
                return;
            }
            if (bankArea.contains(player.getCoordinate())) {
                println("Player is in the bank area, loading last preset...");
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
                println("No archaeology SpotAnimation found nearby.");
            }
        } else {
            if (player.getAnimationId() == -1){
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
            } else{
                println("Already excavating");
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
            eep.interact("Collections");

            if (Execution.delayUntil(10000,() -> Interfaces.isOpen(656))) {
                println("Interface 656 opened, switching to second collection...");

                MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 1, 42991647);  // Switch to second collection
                Execution.delay(1000);

                println("Confirming second collection contribution...");
                MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);  // Submit collection
                Execution.delay(1000);

                println("Second collection submitted. Returning to excavation.");
            } else {
                println("Failed to open interface 656.");
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
            eep.interact("Collections");

            if (Execution.delayUntil(10000,() -> Interfaces.isOpen(656) )) {
                println("Interface 656 opened, interacting with 'Collections'...");

                MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);  // Submit collection
                Execution.delay(1000);

                println("Collection submitted. Returning to excavation.");
            } else {
                println("Failed to open interface 656.");
            }
        } else {
            println("NPC 'Eep' not found.");
        }
    }

    private void checkAndDestroyItems() {
        String[] itemsToDestroy = {
                "Complete tome"
        };

        for (String itemName : itemsToDestroy) {
            if (Backpack.contains(itemName)) {
                println("Found " + itemName + " in backpack. Attempting to destroy...");
                destroyItem(itemName);
            }
        }
    }

    private void destroyItem(String itemName) {
        ResultSet<Component> results = ComponentQuery.newQuery(1473).componentIndex(5).itemName(itemName).results();
        if (!results.isEmpty()) {
            Component itemComponent = results.first();
            itemComponent.interact("Destroy");
            Execution.delayUntil(5000, () -> Interfaces.isOpen(1183));

            if (Interfaces.isOpen(1183)) {
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77529093);
                Execution.delay(600);
            }
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

package net.botwithus;

import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.api.util.collection.Pair;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.events.impl.InventoryUpdateEvent;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.inventories.InventoryContainer;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.NPCAction;
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
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.inventories.Backpack;
import net.botwithus.rs3.game.inventories.Equipment;
import net.botwithus.rs3.util.Regex;

import java.util.*;
import java.util.regex.Pattern;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class CoaezEvents extends LoopingScript {

    public BotState botState = BotState.IDLE;
    public BotState lastActivityState = BotState.IDLE;
    public final Random random = new Random();
    private ScriptConfig config;
    public final CoaezEventGraphicsContext sgc;

    private final Coordinate bankLocation = new Coordinate(605, 1701, 0);
    private final Coordinate excavationLocation = new Coordinate(574, 1738, 0);
    private final Area bankArea = new Area.Circular(bankLocation, 10);
    private final Area excavationArea = new Area.Circular(excavationLocation, 15);
    private final Coordinate summoningLocation = new Coordinate(586, 1742, 0);
    private final Area summoningArea = new Area.Circular(summoningLocation, 15);
    private final Coordinate stairsLocation = new Coordinate(3323,3378,0);
    private final Area stairsArea = new Area.Circular(stairsLocation, 5);
    private final Coordinate tomeLocation = new Coordinate(3318, 3378, 1);
    private final Area tomeArea = new Area.Circular(tomeLocation, 3);
    private final Coordinate eepLocation = new Coordinate(580,1736,0);
    private final Area eepArea = new Area.Circular(eepLocation, 10);


    private static final int MAZE_PORTAL = 131372;
    private static final int BONE_CLUB = 57608;
    private static final int ZOMBIE_IMPLING = 31307;
    private static final int ZOMBIE = 31305;
    private static final int SKELETON = 31306;
    private static final int GATE = 131376;
    private static final int FENCE = 131377;
    private static final int SKARAXXI = 31304;
    private static final Coordinate ENTRANCE_MIN = new Coordinate(698, 1726, 0);
    private static final Coordinate ENTRANCE_MAX = new Coordinate(703, 1727, 0);
    private static final Pattern[] XP_LAMP_PATTERNS = {
            Pattern.compile("Small XP lamp"),
            Pattern.compile("Medium XP lamp"),
            Pattern.compile("Large XP lamp"),
            Pattern.compile("Huge XP lamp")
    };

    private static final Pattern[] BONUS_XP_PATTERNS = {
            Pattern.compile("Bonus XP star \\(small\\)"),
            Pattern.compile("Bonus XP star \\(medium\\)"),
            Pattern.compile("Bonus XP star \\(large\\)"),
            Pattern.compile("Bonus XP star \\(huge\\)")
    };

    private static final Pattern[] PRESENT_PATTERNS = {
            Pattern.compile("Blue Christmas Present"),
            Pattern.compile("White Christmas Present"),
            Pattern.compile("Purple Christmas Present"),
            Pattern.compile("Gold Christmas Present")
    };

    private static final int WHITE_PRESENT = 57900;
    private static final int BLUE_PRESENT = 57901;
    private static final int PURPLE_PRESENT = 57902;
    private static final int GREEN_PRESENT = 57939;
    private static final int GOLD_PRESENT = 57938;

    public final Map<String, Integer> skillActions = new HashMap<>();
    private int selectedSkillActionId = 82772036; // Default to Archaeology
    private final Map<Integer, Integer> confirmationIndices = new HashMap<>();

    public boolean forceCollectionTurnIn = false;

    public enum BotState {
        SUMMONING,
        ARCHAEOLOGY,
        THIEVING,
        PUMPKIN,
        TURNINCOLLECTIONS,
        MAZE,
        FIR_WOODCUTTING,
        TOY_CRAFTING,
        SNOWBALL_FLETCHING,
        ICE_FISHING,
        HOT_CHOCOLATE,
        DECORATIONS,
        BOX_REDEMPTION,
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
    public int tomeCount = 20;
    public boolean handInTomes = false;
    public boolean useMaizeLootTokens;
    public boolean turnInCollections = true;
    private long lastAnimationTime = 0;
    private final int ANIMATION_ID_RITUAL = 6298;
    private final long RITUAL_TIMEOUT_MS = 5000;
    private final Coordinate mazeStartLocation = new Coordinate(624,1715,0);
    private final Area mazeEntranceArea = new Area.Circular(mazeStartLocation, 5);
    boolean usedDoor = false;

    private final Coordinate implingCoords = new Coordinate(707,1726,0);
    private final Area implingArea = new Area.Rectangular(new Coordinate(698,1726,0), new Coordinate(712,1731,0));
    private final Area innerBossArea = new Area.Rectangular(new Coordinate(702,1732,0), new Coordinate(178,1736,0));
    private final Area fenceArea = new Area.Rectangular(new Coordinate(708, 1731, 0), new Coordinate(711, 1732, 0));


    private long lastAnimationFletchingTime = 0;
    private static final int FLETCHING_TIMEOUT = 3000;
    private long lastAnimationCookingTime = 0;
    private boolean needFirewood = false;
    private boolean needMilk = false;
    private boolean needSugar = false;
    private boolean needChocolate = false;
    private boolean needSpice = false;
    private long lastSnowballInteraction = -1;
    private long IDLE_TIMEOUT = 3000;
    private static SceneObject CACHED_SNOW_PILE = null;
    private boolean coolSmokey;

    private static final int UNPAINTED_MARIONETTE = 57928;
    private static final int PAINTED_MARIONETTE = 57929;
    private static final int COMPLETE_MARIONETTE = 57931;
    private static final int INCOMPLETE_MARIONETTE3 = 57927;
    private static final int INCOMPLETE_MARIONETTE = 57925;
    private static final int INCOMPLETE_MARIONETTE2 = 57926;
    private static final int MARIONETTE_HANDLE = 57930;

    private SceneObject CACHED_DECORATION_CRATE = null;
    private SceneObject CACHED_DECORATION_BENCH = null;
    private SceneObject CACHED_DECORATION_BOX = null;
    private boolean isProcessing = false;
    private long lastProcessStart = 0;
    private static final long PROCESSING_TIMEOUT = 150000;
    private boolean receivedFinishedDecoration = false;

    public boolean buySpecialBox = false;
    private static final int SPIRIT_VARBIT = 54788;
    private static final Coordinate HOLLY_LOCATION = new Coordinate(5219, 9791, 0);
    private static final int SPIRIT_REQUIRED = 50000;

    private static final int FISHING_JUJU_VARBIT = 26030;
    private static final int WOODCUTTING_JUJU_VARBIT = 26029;
    private static final int SECONDS_PER_TICK = 15;

    private static final Pattern FISHING_JUJU_PATTERN = Regex.getPatternForContainsString("Perfect juju fishing potion");
    private static final Pattern WOODCUTTING_JUJU_PATTERN = Regex.getPatternForContainsString("Perfect juju woodcutting potion");

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
    public CoaezEvents(String s, ScriptConfig config, ScriptDefinition scriptDefinition) {
        super(s, config, scriptDefinition);
        this.config = config;
        subscribe(ChatMessageEvent.class, this::onChatMessage);
        subscribe(InventoryUpdateEvent.class, this::onInventoryUpdate);
        initializeConfirmationIndices();
        skillActions.put("Attack", 82771982);
        skillActions.put("Constitution", 82771984);
        skillActions.put("Mining", 82771986);
        skillActions.put("Strength", 82771988);
        skillActions.put("Agility", 82771990);
        skillActions.put("Smithing", 82771992);
        skillActions.put("Defense", 82771994);
        skillActions.put("Herblore", 82771996);
        skillActions.put("Fishing", 82771998);
        skillActions.put("Ranged", 82772000);
        skillActions.put("Thieving", 82772002);
        skillActions.put("Cooking", 82772004);
        skillActions.put("Prayer", 82772006);
        skillActions.put("Crafting", 82772008);
        skillActions.put("Firemaking", 82772010);
        skillActions.put("Magic", 82772012);
        skillActions.put("Fletching", 82772014);
        skillActions.put("Woodcutting", 82772016);
        skillActions.put("Runecrafting", 82772018);
        skillActions.put("Slayer", 82772020);
        skillActions.put("Farming", 82772022);
        skillActions.put("Construction", 82772024);
        skillActions.put("Hunter", 82772026);
        skillActions.put("Summoning", 82772028);
        skillActions.put("Dungeoneering", 82772030);
        skillActions.put("Divination", 82772032);
        skillActions.put("Invention", 82772034);
        skillActions.put("Archaeology", 82772036);
        skillActions.put("Necromancy", 82772037);
        this.sgc = new CoaezEventGraphicsContext(this.getConsole(), this);
    }

    private void onInventoryUpdate(InventoryUpdateEvent inventoryUpdateEvent) {
        Item newItem = inventoryUpdateEvent.getNewItem();
        if (newItem != null && newItem.getId() == 56170) {
            receivedFinishedDecoration = true;
        }
    }

    private void onChatMessage(ChatMessageEvent chatMessageEvent) {
        String message = chatMessageEvent.getMessage();
        if (message.contains("You have reached the Spooky token soft cap")) {
            println("Reached max points, stopping collection turn-ins.");
            turnInCollections = false;
        }
        if (message.contains("Could you fetch some more firewood")) {
            println("Need to collect firewood!");
            needFirewood = true;
        }
        else if (message.contains("Let's add a splash of milk")) {
            println("Need to add milk!");
            needMilk = true;
        }
        else if (message.contains("We need a sprinkle of sugar")) {
            println("Need to add sugar!");
            needSugar = true;
        }
        else if (message.contains("need another handful of chocolate chunks")) {
            println("Need to add chocolate!");
            needChocolate = true;
        }
        else if (message.contains("Flavour's a little plain") || message.contains("add another dash of spice")) {
            println("Need to add spice!");
            needSpice = true;
        }
        else if (message.contains("TOO HOT!")){
            println("Cool smokey now");
            coolSmokey = true;
        }
        else if (message.contains("Thank you " + getLocalPlayer().getName())) {
             println("Smokey cooled");
             coolSmokey = false;
        }
    }
    @Override
    public boolean initialize(){
        return false;
    }

    @Override
    public void onLoop() {
        Player player = getLocalPlayer();

        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN) {
            Execution.delay(random.nextLong(2500, 5500))    ;
            return;
        }

        ResultSet<Component> results = ComponentQuery.newQuery(1473).componentIndex(5).itemName("Christmas wrapping paper booster").option("Consume").results();
        Component result = results.first();
        if(result != null) {
            result.interact("Consume");
        }

        handleSpiritShop(player);
        try {
            handleJujuPotions();
        } catch (Exception e) {
            println("Error in juju potions: " + e.getMessage());
        }
        if(forceCollectionTurnIn) {
            handleForceCollectionTurnIn(player);
            return;
        }

        if (sgc.hasStateChanged()) {
            sgc.saveConfig();
        }

        if(useMaizeLootTokens){
            handleMaizeMazeLootTokens();
        }

        switch (botState) {
            case BOX_REDEMPTION:
                handleBoxRedemption(player);
                break;
            case DECORATIONS:
                handleDecorations(player);
                break;
            case HOT_CHOCOLATE:
                handleHotChocolate(player);
                return;
            case ICE_FISHING:
                handleIceFishing(player);
                return;
            case SNOWBALL_FLETCHING:
                handleSnowballFletching(player);
                return;
            case FIR_WOODCUTTING:
                handleFirWoodcutting(player);
                return;
            case TOY_CRAFTING:
                handleToyCrafting(player);
                return;
            case MAZE:
                handleMaze(player);
                return;
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

    private void handleBoxRedemption(Player player) {
        PresentInventoryState state = checkPresentInventoryState();

        if (state.totalPresentCount == 0) {
            handleXPItems();
            handleBanking();
            botState = BotState.IDLE;
            setActive(false);
            return;
        }

	if (state.totalPresentCount > 0 && getFreeSlots() < 3) {
            handleXPItems();
            handleBanking();
        }

        if (state.whitePresentsCount > 0) {
            println("Opening White Christmas Present...");
            Backpack.interact(WHITE_PRESENT, "Open");
            return;
        }

        if (state.bluePresentsCount > 0) {
            println("Opening Blue Christmas Present...");
            Backpack.interact(BLUE_PRESENT, "Open");
            return;
        }

        if (state.purplePresentsCount > 0) {
            println("Opening Purple Christmas Present...");
            Backpack.interact(PURPLE_PRESENT, "Open");
            return;
        }

        if (state.greenPresentsCount > 0) {
            println("Opening Green Christmas Present...");
            Backpack.interact(GREEN_PRESENT, "Open");
            return;
        }

        if (state.goldPresentsCount > 0) {
            println("Opening Gold Christmas Present...");
            Backpack.interact(GOLD_PRESENT, "Open");
            return;
        }

    }

    private PresentInventoryState checkPresentInventoryState() {
        return new PresentInventoryState();
    }

    private class PresentInventoryState {
        final int whitePresentsCount;
        final int bluePresentsCount;
        final int purplePresentsCount;
        final int greenPresentsCount;
        final int goldPresentsCount;
        final int totalPresentCount;
        final int totalItems;
        final int freeSlots;

        public PresentInventoryState() {

			println("Creating new present inventory state");

            this.whitePresentsCount = countItemsByIdInBackpack(WHITE_PRESENT); //countItemsByIdInBackpack("White Christmas Present");
            this.bluePresentsCount = countItemsByIdInBackpack(BLUE_PRESENT); //countItemsByIdInBackpack("Blue Christmas Present");
            this.purplePresentsCount = countItemsByIdInBackpack(PURPLE_PRESENT); //countItemsByIdInBackpack("Purple Christmas Present");
            this.greenPresentsCount = countItemsByIdInBackpack(GREEN_PRESENT); //countItemsByIdInBackpack("Green Christmas Present");
            this.goldPresentsCount = countItemsByIdInBackpack(GOLD_PRESENT); //countItemsByIdInBackpack("Gold Christmas Present");

            this.totalPresentCount = whitePresentsCount + bluePresentsCount + purplePresentsCount + greenPresentsCount + goldPresentsCount;
            this.totalItems = getTotalItemCount();
            this.freeSlots = getFreeSlots();
        }

        public void logState() {
            println("==== Present Box Inventory State ====");
            println("Blue presents: " + bluePresentsCount);
            println("White presents: " + whitePresentsCount);
            println("Purple presents: " + purplePresentsCount);
            println("Gold presents: " + goldPresentsCount);
            println("Total presents: " + totalPresentCount);
            println("Free slots: " + freeSlots);
            println("Total items: " + totalItems);
            println("===================================");
        }
    }

    private void handleXPItems() {
        // Handle XP Lamps
        for (Pattern lampPattern : XP_LAMP_PATTERNS) {
            ResultSet<Component> lampResults = ComponentQuery.newQuery(1473)
                    .componentIndex(5)
                    .itemName(lampPattern)
                    .option("Rub")
                    .results();

            Component lamp = lampResults.first();
            if (lamp != null) {
                println("Using " + lamp.getText() + "...");
                if (lamp.interact("Rub")) {
                    boolean interfaceOpen = Execution.delayUntil(5000, () -> Interfaces.isOpen(678) || Interfaces.isOpen(1263));
                    Execution.delay(random.nextLong(600, 1200));
                    if (interfaceOpen) {
                        println("Selecting skill with lamp action ID: " + sgc.getLampSkillActionId());
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, sgc.getLampSkillActionId());
                        Execution.delay(random.nextLong(800, 1400));

                        if (Interfaces.isOpen(678)) {
                            println("Selecting use all option");
                            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 44433427);
                            Execution.delay(random.nextLong(800, 1400));
                        }

                        int confirmIndex = confirmationIndices.get(sgc.getLampSkillActionId());
                        println("Using confirmation index: " + confirmIndex);
                        MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, confirmIndex, 82772042);
                        Execution.delay(random.nextLong(800, 1400));
                    }
                }
            }
        }

        for (Pattern starPattern : BONUS_XP_PATTERNS) {
            ResultSet<Component> starResults = ComponentQuery.newQuery(1473)
                    .componentIndex(5)
                    .itemName(starPattern)
                    .option("Choose skill")
                    .results();

            Component star = starResults.first();
            if (star != null) {
                println("Using " + star.getText() + "...");
                if (star.interact("Choose skill")) {
                    boolean interfaceOpen = Execution.delayUntil(5000, () -> Interfaces.isOpen(678) || Interfaces.isOpen(1263));
                    if (interfaceOpen) {
                        println("Selecting skill with star action ID: " + sgc.getStarSkillActionId());
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, sgc.getStarSkillActionId());
                        Execution.delay(random.nextLong(800, 1400));

                        if (Interfaces.isOpen(678)) {
                            println("Selecting use all option");
                            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 44433427);
                            Execution.delay(random.nextLong(800, 1200));
                        }

                        int confirmIndex = confirmationIndices.get(sgc.getStarSkillActionId());
                        println("Using confirmation index: " + confirmIndex);
                        MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, confirmIndex, 82772042);
                        Execution.delay(random.nextLong(800, 1400));
                    }
                }
            }
        }

        if (getFreeSlots() < 24) {
            handleBanking();
        }
    }

    private void initializeConfirmationIndices() {
        confirmationIndices.put(82771982, 1);  // Attack
        confirmationIndices.put(82771984, 6);  // Constitution
        confirmationIndices.put(82771986, 13); // Mining
        confirmationIndices.put(82771988, 2);  // Strength
        confirmationIndices.put(82771990, 8);  // Agility
        confirmationIndices.put(82771992, 14); // Smithing
        confirmationIndices.put(82771994, 5);  // Defense
        confirmationIndices.put(82771996, 9);  // Herblore
        confirmationIndices.put(82771998, 15); // Fishing
        confirmationIndices.put(82772000, 3);  // Ranged
        confirmationIndices.put(82772002, 10); // Thieving
        confirmationIndices.put(82772004, 16); // Cooking
        confirmationIndices.put(82772006, 7);  // Prayer
        confirmationIndices.put(82772008, 11); // Crafting
        confirmationIndices.put(82772010, 17); // Firemaking
        confirmationIndices.put(82772012, 4);  // Magic
        confirmationIndices.put(82772014, 19); // Fletching
        confirmationIndices.put(82772016, 18); // Woodcutting
        confirmationIndices.put(82772018, 12); // Runecrafting
        confirmationIndices.put(82772020, 20); // Slayer
        confirmationIndices.put(82772022, 21); // Farming
        confirmationIndices.put(82772024, 22); // Construction
        confirmationIndices.put(82772026, 23); // Hunter
        confirmationIndices.put(82772028, 24); // Summoning
        confirmationIndices.put(82772030, 25); // Dungeoneering
        confirmationIndices.put(82772032, 26); // Divination
        confirmationIndices.put(82772034, 27); // Invention
        confirmationIndices.put(82772036, 28); // Archaeology
        confirmationIndices.put(82772037, 29); // Necromancy
    }

    private void handleBanking() {
        println("Banking items...");
        if(!Bank.isOpen()){
            Bank.open();
        }
        Execution.delayUntil(5000, Bank::isOpen);
        Execution.delay(random.nextLong(600, 1200));
        if (Bank.isOpen()) {
            Bank.depositAll();
            Execution.delay(random.nextLong(600, 1200));
            Bank.close();
        }
    }

    private boolean handleJujuPotions() {
        try {
            int fishingValue = VarManager.getVarbitValue(FISHING_JUJU_VARBIT);
            int woodcuttingValue = VarManager.getVarbitValue(WOODCUTTING_JUJU_VARBIT);

            if (fishingValue == 0 || woodcuttingValue == 0) {
                InventoryContainer container = Backpack.container();
                List<Item> items = container.getItems();

                if (fishingValue == 0) {
                    for (Item item : items) {
                        String itemName = item != null ? item.getName() : null;
                        if (itemName != null && FISHING_JUJU_PATTERN.matcher(itemName).matches()) {
                            return Backpack.interact(itemName, "Drink");
                        }
                    }
                }

                if (woodcuttingValue == 0) {
                    for (Item item : items) {
                        String itemName = item != null ? item.getName() : null;
                        if (itemName != null && WOODCUTTING_JUJU_PATTERN.matcher(itemName).matches()) {
                            return Backpack.interact(itemName, "Drink");
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            println("Error in handleJujuPotions: " + e);
            return false;
        }
    }

    private void handleSpiritShop(Player player) {
        if (!buySpecialBox) {
            return;
        }

        int currentSpirit = VarManager.getVarbitValue(SPIRIT_VARBIT);
        println("Current spirit: " + currentSpirit);

        if (currentSpirit >= SPIRIT_REQUIRED) {
            if (!player.getCoordinate().equals(HOLLY_LOCATION)) {
                println("Moving to Holly...");
                Movement.walkTo(HOLLY_LOCATION.getX(), HOLLY_LOCATION.getY(), false);
                Execution.delayUntil(10000, () -> player.getCoordinate().equals(HOLLY_LOCATION));
            }

            EntityResultSet<Npc> results = NpcQuery.newQuery()
                    .byType(30753)
                    .results();
            Npc holly = results.nearest();
            if (holly != null && holly.interact(NPCAction.NPC1)) {
                println("Opening spirit shop...");
                if (Execution.delayUntil(5000, () -> Interfaces.isOpen(1594))) {
                    println("Buying first special box...");
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 10, 104464403);
                    Execution.delay(random.nextLong(1200, 1800));
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 104464438);
                    Execution.delay(random.nextLong(1200, 1800));

                    println("Buying second special box...");
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 10, 104464403);
                    Execution.delay(random.nextLong(1200, 1800));
                    MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 104464438);

                    println("Purchased both special boxes!");
                }
            }
            currentSpirit = VarManager.getVarbitValue(SPIRIT_VARBIT);
            if (currentSpirit >= SPIRIT_REQUIRED){
                println("Didn't spend points, retrying");
                handleSpiritShop(player);
            }

        }
    }

    private void handleDecorations(Player player) {
        if (CACHED_DECORATION_CRATE == null || CACHED_DECORATION_BENCH == null || CACHED_DECORATION_BOX == null) {
            initializeDecorationObjects();
            return;
        }

        int unfinishedCount = countDecorationItems(56168, 56169);
        int finishedCount = countDecorationItems(56170);

        if (unfinishedCount > 0 && player.getAnimationId() == -1) {
            receivedFinishedDecoration = false;
            println("Creating decorations from unfinished items...");
            if (CACHED_DECORATION_BENCH.interact("Create")) {
                Execution.delayUntil(PROCESSING_TIMEOUT, () -> receivedFinishedDecoration);
                return;
            }
        }

        if (finishedCount >= 20) {
            println("Depositing finished decorations...");
            CACHED_DECORATION_BOX.interact("Deposit all");
            Execution.delayUntil(5000, Backpack::isEmpty);
            return;
        }

        if (unfinishedCount == 0 && !Backpack.isFull()) {
            println("Collecting unfinished decorations...");
            while (!Backpack.isFull()) {
                if (CACHED_DECORATION_CRATE.interact("Take from")) {
                    Execution.delay(random.nextLong(200, 400));
                }
            }
            println("Inventory full of unfinished decorations");
        }
    }

    private int countDecorationItems(int... itemIds) {
        int count = 0;
        for (Item item : Backpack.container().getItems()) {
            if (item != null) {
                for (int id : itemIds) {
                    if (item.getId() == id) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void initializeDecorationObjects() {
        if (CACHED_DECORATION_CRATE == null) {
            EntityResultSet<SceneObject> crateResults = SceneObjectQuery.newQuery()
                    .name("Crate of unfinished decorations")
                    .option("Take from")
                    .results();
            CACHED_DECORATION_CRATE = crateResults.nearest();
            println("Decoration crate " + (CACHED_DECORATION_CRATE != null ? "cached successfully!" : "cache failed!"));
        }

        if (CACHED_DECORATION_BENCH == null) {
            EntityResultSet<SceneObject> benchResults = SceneObjectQuery.newQuery()
                    .name("Decoration bench")
                    .option("Create")
                    .results();
            CACHED_DECORATION_BENCH = benchResults.nearest();
            println("Decoration bench " + (CACHED_DECORATION_BENCH != null ? "cached successfully!" : "cache failed!"));
        }
        if(CACHED_DECORATION_BOX == null) {
            EntityResultSet<SceneObject> boxResults = SceneObjectQuery.newQuery()
                    .name("Crate of finished decorations")
                    .option("Deposit all")
                    .results();
            CACHED_DECORATION_BOX = boxResults.nearest();
            println("Crate of finished decorations " + (CACHED_DECORATION_BOX != null ? "cached successfully!" : "cache failed!"));
        }
    }

    private void handleHotChocolate(Player player) {
        if (needFirewood) {
            println("Need firewood, searching...");
            EntityResultSet<SceneObject> firewoodResults = SceneObjectQuery.newQuery()
                    .name("Firewood")
                    .option("Take")
                    .results();

            SceneObject firewood = firewoodResults.nearest();
            if (firewood != null) {
                println("Found firewood at: " + firewood.getCoordinate());
                firewood.interact("Take");
                Execution.delayUntil(5000, () -> Backpack.contains("Firewood"));
                needFirewood = false;
                println("Got firewood: " + Backpack.contains("Firewood"));
                return;
            } else {
                println("No firewood found!");
            }
        }

        if (needMilk) {
            println("Need milk, searching...");
            EntityResultSet<SceneObject> milkResults = SceneObjectQuery.newQuery()
                    .name("Milk")
                    .option("Take")
                    .results();

            SceneObject milk = milkResults.nearest();
            if (milk != null) {
                println("Found milk at: " + milk.getCoordinate());
                milk.interact("Take");
                Execution.delayUntil(5000, () -> Backpack.contains("Milk"));
                needMilk = false;
                println("Got milk: " + Backpack.contains("Milk"));
                return;
            } else {
                println("No milk found!");
            }
        }

        if (needSugar) {
            println("Need sugar, searching...");
            EntityResultSet<SceneObject> sugarResults = SceneObjectQuery.newQuery()
                    .name("Sugar")
                    .option("Take")
                    .results();

            SceneObject sugar = sugarResults.nearest();
            if (sugar != null) {
                println("Found sugar at: " + sugar.getCoordinate());
                sugar.interact("Take");
                Execution.delayUntil(5000, () -> Backpack.contains("Sugar"));
                needSugar = false;
                println("Got sugar: " + Backpack.contains("Sugar"));
                return;
            } else {
                println("No sugar found!");
            }
        }

        if (needChocolate) {
            println("Need chocolate, searching...");
            EntityResultSet<SceneObject> chocolateResults = SceneObjectQuery.newQuery()
                    .name("Chocolate")
                    .option("Take")
                    .results();

            SceneObject chocolate = chocolateResults.nearest();
            if (chocolate != null) {
                println("Found chocolate at: " + chocolate.getCoordinate());
                chocolate.interact("Take");
                Execution.delayUntil(5000, () -> Backpack.contains("Chocolate chunks"));
                needChocolate = false;
                println("Got chocolate: " + Backpack.contains("Chocolate chunks"));
                return;
            } else {
                println("No chocolate found!");
            }
        }

        if (needSpice) {
            println("Need spice, searching...");
            EntityResultSet<SceneObject> spiceResults = SceneObjectQuery.newQuery()
                    .name("Spices")
                    .option("Take")
                    .results();

            SceneObject spice = spiceResults.nearest();
            if (spice != null) {
                println("Found spice at: " + spice.getCoordinate());
                spice.interact("Take");
                Execution.delayUntil(5000, () -> Backpack.contains("Spice"));
                needSpice = false;
                println("Got spice: " + Backpack.contains("Spice"));
                return;
            } else {
                println("No spice found!");
            }
        }


        // Update last animation time if we're currently animating
        if (player.getAnimationId() != -1) {
            lastAnimationCookingTime = System.currentTimeMillis();
            return;
        }

        long timeSinceLastAnim = System.currentTimeMillis() - lastAnimationCookingTime;
        if (player.getAnimationId() == -1 && timeSinceLastAnim >= random.nextInt(1000, 2000)) {
            println("Player is idle and timeout reached (" + timeSinceLastAnim + "ms), searching for pot...");
            EntityResultSet<SceneObject> potResults = SceneObjectQuery.newQuery()
                    .id(131826)
                    .option("Assist Aoife")
                    .results();

            SceneObject pot = potResults.nearest();
            if (pot != null) {
                println("Found pot at: " + pot.getCoordinate());
                pot.interact("Assist Aoife");
                println("Interacted with pot");
                Execution.delay(random.nextLong(600, 1200));
            } else {
                println("No pot found!");
            }
        }
    }

    private void handleIceFishing(Player player) {
        if (Backpack.isFull()) {
            EntityResultSet<SceneObject> barrelResults = SceneObjectQuery.newQuery()
                    .name("Barrel of fish")
                    .option("Deposit all")
                    .results();

            SceneObject barrel = barrelResults.nearest();
            if (barrel != null) {
                println("Depositing fish...");
                barrel.interact("Deposit all");
                Execution.delayUntil(5000, () -> !Backpack.isFull());
                return;
            }
        }

        if (player.getAnimationId() == -1 && !Backpack.isFull()) {
            EntityResultSet<Npc> fishingSpotResults = NpcQuery.newQuery()
                    .name("Icy fishing spot")
                    .option("Catch")
                    .results();
            Npc fishingSpot = fishingSpotResults.nearest();
            if (fishingSpot != null) {
                println("Starting to fish...");
                fishingSpot.interact("Catch");
                Execution.delay(random.nextLong(3000, 6000));

            }
        } else {
            Execution.delay(random.nextLong(1000, 3000));
        }
    }

    private InventoryState checkInventoryState() {
        return new InventoryState();
    }

    private void handleToyCrafting(Player player) {
        InventoryState state = checkInventoryState();
        state.logState();

        if (state.shouldTurnInComplete()) {
            handleTurnIn(player);
            return;
        }

        if (state.shouldAssembleMarionettes()) {
            handleAssembly(player);
            return;
        }

        if (state.shouldPaintMarionettes()) {
            handlePainting(player);
            return;
        }

        if (state.shouldCarveWood()) {
            handleCarving(player);
            return;
        }

        if (state.shouldGetWood()) {
            handleGettingWood(player);
            return;
        }
    }

    private class InventoryState {
        final int woodCount;
        final int unpaintedCount;
        final int paintedCount;
        final int handleCount;
        final int completeCount;
        final int incompleteCount;
        final int totalItems;
        final int freeSlots;

        public InventoryState() {
            println("Creating new inventory state");

            this.woodCount = countItemsInBackpack("Dry fir wood");
            this.unpaintedCount = countItemsByIdInBackpack(UNPAINTED_MARIONETTE);
            this.paintedCount = countItemsByIdInBackpack(PAINTED_MARIONETTE);
            this.handleCount = countItemsByIdInBackpack(MARIONETTE_HANDLE);
            this.completeCount = countItemsByIdInBackpack(COMPLETE_MARIONETTE);
            this.incompleteCount = countItemsByIdInBackpack(INCOMPLETE_MARIONETTE)
                    + countItemsByIdInBackpack(INCOMPLETE_MARIONETTE2)
                    + countItemsByIdInBackpack(INCOMPLETE_MARIONETTE3);

            this.totalItems = getTotalItemCount();  // Get the total number of items (excluding free slots)
            this.freeSlots = getFreeSlots();  // Get the count of free slots explicitly
        }


        public void logState() {
            println("==== Toy Crafting Inventory State ====");
            println("Wood: " + woodCount);
            println("Unpainted: " + unpaintedCount);
            println("Painted: " + paintedCount);
            println("Handles: " + handleCount);
            println("Complete: " + completeCount);
            println("Incomplete: " + incompleteCount);
            println("Free slots: " + freeSlots);
            println("Other items: " + (totalItems - (woodCount + unpaintedCount + paintedCount + handleCount + completeCount + incompleteCount)));
            println("===================================");
        }


        public boolean shouldTurnInComplete() {
            if (completeCount > 0) {
                println("Decision: Turn in completed marionettes");
                return true;
            }
            return false;
        }

        public boolean shouldAssembleMarionettes() {
            if (paintedCount > 0 && handleCount > 0) {
                println("Decision: Assemble marionettes (have painted + handles)");
                return true;
            }
            return false;
        }

        public boolean shouldPaintMarionettes() {
            if (unpaintedCount > 0) {
                println("Decision: Paint marionettes (have unpainted)");
                return true;
            }
            return false;
        }

        public boolean shouldCarveWood() {
            if (woodCount > 0) {
                println("Decision: Carve wood (have " + woodCount + " wood)");
                return true;
            }
            return false;
        }

        public boolean shouldGetWood() {
            // Only get wood if we have space AND can't do any other actions
            if (freeSlots >= 6 &&
                    !shouldTurnInComplete() &&
                    !shouldAssembleMarionettes() &&
                    !shouldPaintMarionettes() &&
                    !shouldCarveWood()) {
                println("Decision: Get wood (can't do other actions and have space)");
                return true;
            }
            return false;
        }


    }

    private void handleTurnIn(Player player) {
        println("Turning in completed marionettes...");
        EntityResultSet<Npc> maeveResults = NpcQuery.newQuery()
                .name("Maeve")
                .option("Talk to")
                .results();

        Npc maeve = maeveResults.nearest();
        if (maeve != null) {
            maeve.interact("Talk to");
            Execution.delay(random.nextLong(2000, 4000));
        }
    }

    private void handleAssembly(Player player) {
        println("Starting marionette assembly...");
        EntityResultSet<SceneObject> finishingBenchResults = SceneObjectQuery.newQuery()
                .name("Finishing bench")
                .option("Assemble")
                .results();

        SceneObject finishingBench = finishingBenchResults.nearest();
        if (finishingBench != null) {
            finishingBench.interact("Assemble");
            Execution.delayUntil(90000, () -> !Backpack.contains(PAINTED_MARIONETTE) || !Backpack.contains(MARIONETTE_HANDLE));
            Execution.delay(random.nextLong(600, 2000));
        }
    }

    private void handlePainting(Player player) {
        println("Starting marionette painting...");
        EntityResultSet<SceneObject> paintingBenchResults = SceneObjectQuery.newQuery()
                .name("Painting bench")
                .option("Paint")
                .results();

        SceneObject paintingBench = paintingBenchResults.nearest();
        if (paintingBench != null) {
            paintingBench.interact("Paint");
            Execution.delayUntil(30000, () -> !Backpack.contains(UNPAINTED_MARIONETTE));
            Execution.delay(random.nextLong(600, 2000));
        }
    }

    private void handleCarving(Player player) {
        println("Starting wood carving...");
        EntityResultSet<SceneObject> carvingBenchResults = SceneObjectQuery.newQuery()
                .name("Carving bench")
                .option("Carve")
                .results();

        SceneObject carvingBench = carvingBenchResults.nearest();
        if (carvingBench != null) {
            carvingBench.interact("Carve");
            Execution.delayUntil(120000, () -> !Backpack.contains("Dry fir wood"));
            Execution.delay(random.nextLong(600, 2000));
        }
    }

    private void handleGettingWood(Player player) {
        println("Getting more wood...");
        EntityResultSet<SceneObject> crateResults = SceneObjectQuery.newQuery()
                .name("Crate of wood")
                .option("Take from")
                .results();

        SceneObject crate = crateResults.nearest();
        if (crate != null) {
            crate.interact("Take from");
            Execution.delayUntil(15000, () -> Backpack.contains("Dry fir wood") && Backpack.isFull());
            Execution.delay(random.nextLong(600, 2000));
        }
    }

    private int countItemsInBackpack(String itemName) {
        int count = 0;
        println("Checking backpack for: " + itemName);
        List<Item> items = Backpack.container().getItems();

        for (Item item : items.toArray(new Item[0])) {
            if (item != null && item.getSlot() != -1) {
                println("Found item in slot " + item.getSlot() + ": ID=" + item.getId());

                if (item.getName() != null && item.getName().equals(itemName)) {
                    count++;
                }
            }
        }

        println("Total count for " + itemName + ": " + count);
        return count;
    }

    private int countItemsByIdInBackpack(int itemId) {
        int count = 0;
        println("Checking backpack for ID: " + itemId);
        List<Item> items = Backpack.container().getItems();

        for (Item item : items.toArray(new Item[0])) {
            if (item != null && item.getSlot() != -1 && item.getId() == itemId) {  // Exclude items in free slots (-1)
                count += item.getStackSize();
                println("Found item with ID " + itemId + " in slot " + item.getSlot() + ", current count: " + count);
            }
        }

        println("Total count for ID " + itemId + ": " + count);
        return count;
    }

    private int getTotalItemCount() {
        int count = 0;
        println("Counting total items in backpack");
        List<Item> items = Backpack.container().getItems();
        for (Item item : items) {
            if (item != null && item.getId() != -1) {
                count++;
                println("Found item in slot " + item.getSlot() + ": ID=" + item.getId());
            }
        }
        println("Total items in backpack: " + count);
        return count;
    }

    private int getFreeSlots() {
        int freeSlotCount = 0;
        List<Item> items = Backpack.container().getItems();

        for (Item item : items) {
            if (item != null && item.getId() == -1) {
                freeSlotCount++;
            }
        }

        println("Free slots: " + freeSlotCount);
        return freeSlotCount;
    }

    private void handleFirWoodcutting(Player player) {

        if (!Backpack.isFull() && player.getAnimationId() == -1) {
            EntityResultSet<SceneObject> treeResults = SceneObjectQuery.newQuery()
                    .name("Fir")
                    .option("Chop down")
                    .hidden(false)
                    .results();

            SceneObject tree = treeResults.nearest();
            if (tree != null && !tree.isHidden()) {
                println("Chopping fir tree...");
                tree.interact("Chop down");
                Execution.delayUntil(random.nextInt(3000, 6000), Backpack::isFull);
                return;
            }
        }

        if (Backpack.contains("Fir logs") && Backpack.isFull()) {
            EntityResultSet<SceneObject> stumpResults = SceneObjectQuery.newQuery()
                    .name("Log splitting stump")
                    .option("Split logs")
                    .results();

            SceneObject stump = stumpResults.nearest();
            if (stump != null) {
                println("Splitting fir logs...");
                stump.interact("Split logs");
                Execution.delayUntil(90000, () -> !Backpack.contains("Fir logs"));
                Execution.delay(random.nextLong(600, 2000));
                return;
            }
        }
        if (Backpack.contains("Split fir logs")) {
            EntityResultSet<SceneObject> stockpileResults = SceneObjectQuery.newQuery()
                    .name("Split log stockpile")
                    .option("Deposit all")
                    .results();

            SceneObject stockpile = stockpileResults.nearest();
            if (stockpile != null) {
                println("Depositing split logs...");
                stockpile.interact("Deposit all");
                Execution.delayUntil(10000, () -> !Backpack.contains("Split fir logs"));
                return;
            }
        }

        Execution.delay(random.nextLong(600, 2000));
    }

    private void initializeSnowPile() {
        if (CACHED_SNOW_PILE == null) {
            EntityResultSet<SceneObject> snowPileResults = SceneObjectQuery.newQuery()
                    .name("Pile of snow")
                    .option("Create snowball")
                    .results();
            CACHED_SNOW_PILE = snowPileResults.nearest();
            if (CACHED_SNOW_PILE != null) {
                println("Snow pile cached successfully!");
            } else {
                println("Failed to cache snow pile!");
            }
        }
    }

    private void handleSnowballFletching(Player player) {
        if (CACHED_SNOW_PILE == null) {
            initializeSnowPile();
            return;
        }
        if(coolSmokey){
            EntityResultSet<Npc> smokeyResults = NpcQuery.newQuery()
                    .name("Smokey")
                    .option("Pelt snowball")
                    .results();
            Npc smokey = smokeyResults.nearest();
            if (smokey != null) {
                println("Found Smokey! Equipping snowballs if needed and cooling him...");

                if (!Equipment.contains("Snowball")) {
                    if (Backpack.contains("Snowball")) {
                        Backpack.interact("Snowball", "Wield");
                        Execution.delay(random.nextLong(600, 1200));
                    }
                }

                smokey.interact("Pelt snowball");
                Execution.delay(random.nextLong(2400, 3000));
                return;
            }
        }


        if (player.getAnimationId() != -1) {
            println("Player already creating snowballs");
            Execution.delay(3600);
            return;
        }

        if (CACHED_SNOW_PILE != null && player.getAnimationId() == -1) {
            println("Creating snowballs...");
            CACHED_SNOW_PILE.interact("Create snowball");
            Execution.delayUntil(3000, () -> player.getAnimationId() != -1);
        }
    }

    private void handleMaze(Player player) {
        if (!mazeEntranceArea.contains(player.getCoordinate()) && !usedDoor) {
            println("Moving to maze entrance at " + mazeEntranceArea);
            Movement.walkTo(mazeEntranceArea.getCoordinate().getX(), mazeEntranceArea.getCoordinate().getY(), false);
            Execution.delayUntil(5000, () -> mazeEntranceArea.contains(player));
            return;
        }

        EntityResultSet<SceneObject> mazeDoorResults = SceneObjectQuery.newQuery().name("Maize Maze portal").option("Enter").results();
        SceneObject mazeEntranceDoor = mazeDoorResults.nearest();

        if (mazeEntranceDoor != null && !usedDoor) {
            println("Entering the maze...");
            mazeEntranceDoor.interact("Enter");
            Execution.delayUntil(5000, () -> player.getCoordinate().getRegionId() != 2330);
            Execution.delay(random.nextLong(2000, 3000));
            usedDoor = true;
        }

        if (!implingArea.contains(player.getCoordinate())) {
            println("Not in the impling area, navigate to the area manually.");
            Execution.delay(random.nextLong(2000, 3000));
            return;
        }

        if (!Backpack.contains("Bone club", 10)) {
            EntityResultSet<Npc> results = NpcQuery.newQuery().name("Zombie impling").option("Catch").inside(implingArea).results();
            Npc impling = results.nearest();

            if (impling != null && impling.interact("Catch")) {
                println("Catching a Zombie impling...");
                Execution.delay(random.nextLong(1200, 2000));
                Execution.delayUntil(3000, () -> player.getAnimationId() == -1);
            } else {
                println("No Zombie impling found, waiting...");
                Execution.delay(random.nextLong(100, 200));
            }
        } else {
            Execution.delayUntil(4000, () -> getLocalPlayer().getAnimationId() == -1);
            println("Collected 10 Bone clubs. Preparing to jump over the fence...");
            EntityResultSet<SceneObject> fenceResults = SceneObjectQuery.newQuery().name("Fence obstacle").option("Jump over").inside(fenceArea).results();
            SceneObject fence = fenceResults.nearest();

            if (fence != null && fence.interact("Jump over")) {
                println("Jumping over the fence...");
                Execution.delayUntil(8000, () -> player.getAnimationId() == 19972);
                Execution.delayUntil(2000, () -> player.getAnimationId() != 19972);
            }

            println("Preparing to fight the boss...");

            while (Backpack.contains("Bone club")) {
                println("Using Bone Clubs to spook the boss...");
                EntityResultSet<Npc> bossResults = NpcQuery.newQuery().name("Solak-o'-lantern").option("Spook (melee)").results();
                Npc boss = bossResults.nearest();

                if (boss != null) {
                    if (boss.interact("Spook (melee)")) {
                        println("Spooked Solak");
                        Execution.delay(random.nextLong(1000, 1200));
                    }
                } else {
                    println("Could not find Skaraxxi!");
                    break;
                }
            }

            println("Out of Bone Clubs or boss defeated!");
            Execution.delay(random.nextLong(5000, 8000));
            usedDoor = false;
        }
    }

    private void waitForStillness() {
        Execution.delay(random.nextLong(1500, 2000));
        Player player = getLocalPlayer();
        while (player != null && (player.isMoving() || player.getAnimationId() != -1)) {
            Execution.delay(random.nextLong(500, 1000));
        }
    }

    private void navigateWithEnemyAvoidance(Player player, Area targetArea) {
        if (!player.isMoving() && player.getAnimationId() != -1) {
            Npc enemy = NpcQuery.newQuery()
                    .id(ZOMBIE).id(SKELETON)
                    .results()
                    .nearest();

            if (enemy != null && enemy.distanceTo(player) <= 2) {
                println("Spooking enemy!");
                enemy.interact("Spook");
                Execution.delay(random.nextLong(2500, 3000));
            }

            Coordinate destination = targetArea.getRandomWalkableCoordinate();
            Movement.walkTo(destination.getX(), destination.getY(), false);
            println("Navigating to " + destination);

            Npc impling = NpcQuery.newQuery()
                    .id(ZOMBIE_IMPLING)
                    .results()
                    .nearest();

            if (impling != null) {
                println("Catching zombie impling!");
                impling.interact("Catch");
                Execution.delay(random.nextLong(500, 1000));
            }
        }

        Execution.delay(random.nextLong(250, 1000));
    }

    private void handleMaizeMazeLootTokens() {
        String[] collectionItems = {
                "Maize Maze loot token (double)", "Maize Maze loot token (triple)"
        };

        for (String item : collectionItems) {
            if (Backpack.contains(item)) {
                Backpack.interact(item, "Redeem All");
                Execution.delay(random.nextLong(600, 1200));
                break;
            }
        }
    }

    private void handleCollectionTurnIn(Player player) {

        if(!bankArea.contains(player)){
            Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
            Execution.delay(random.nextLong(6000, 10000));
        }
        if (!Bank.isOpen()) {
            println("Opening the bank...");
            Bank.open();
            Execution.delayUntil(5000, Bank::isOpen);
            Bank.depositAll();
            Execution.delay(random.nextLong(600, 1200));
        } else {
            println("Can't open bank");
            return;
        }

        String[] firstCollectionItems = {
                "Soiled storybook", "Polished horn", "Dusty snow globe", "Ruined letter", "Strange coins"
        };

        String[] secondCollectionItems = {
                "Ancient skull", "Ancient torso", "Ancient left arm", "Ancient right arm", "Ancient left leg", "Ancient right leg"
        };

        int firstCollectionItemCount = firstCollectionItems.length;
        int secondCollectionItemCount = secondCollectionItems.length;

        println("Calculating available collections from the bank...");
        int firstCollectionAvailable = calculateAvailableCollections(firstCollectionItems, firstCollectionItemCount);
        int secondCollectionAvailable = calculateAvailableCollections(secondCollectionItems, secondCollectionItemCount);

        println("You have enough items to complete " + firstCollectionAvailable + " first collections.");
        println("You have enough items to complete " + secondCollectionAvailable + " second collections.");

        int totalFreeSpace = 28;

        while (firstCollectionAvailable > 0 || secondCollectionAvailable > 0) {
            int maxCollectionsForFirst = Math.min(totalFreeSpace / firstCollectionItemCount, firstCollectionAvailable);
            int maxCollectionsForSecond = Math.min(totalFreeSpace / secondCollectionItemCount, secondCollectionAvailable);

            if (maxCollectionsForFirst == 0 && maxCollectionsForSecond == 0) {
                println("No more collections to withdraw. Stopping.");
                setBotState(BotState.IDLE);
                return;
            }

            int collectionsToWithdrawFirst = Math.min(maxCollectionsForFirst, totalFreeSpace / firstCollectionItemCount);
            int collectionsToWithdrawSecond = Math.min(maxCollectionsForSecond, (totalFreeSpace - collectionsToWithdrawFirst * firstCollectionItemCount) / secondCollectionItemCount);

            println("Withdrawing " + collectionsToWithdrawFirst + " first collections and " + collectionsToWithdrawSecond + " second collections.");
            if (collectionsToWithdrawFirst > 0) {
                withdrawMaxCollectionItems(firstCollectionItems, collectionsToWithdrawFirst);
                totalFreeSpace -= collectionsToWithdrawFirst * firstCollectionItemCount;
                firstCollectionAvailable -= collectionsToWithdrawFirst;
            }

            if (collectionsToWithdrawSecond > 0) {
                withdrawMaxCollectionItems(secondCollectionItems, collectionsToWithdrawSecond);
                totalFreeSpace -= collectionsToWithdrawSecond * secondCollectionItemCount;
                secondCollectionAvailable -= collectionsToWithdrawSecond;
            }

            Bank.close();

            if (collectionsToWithdrawFirst > 0) {
                println("Turning in first collection.");
                handleCollectionSubmission(player);
            }

            if (collectionsToWithdrawSecond > 0) {
                println("Turning in second collection.");
                handleSecondCollectionSubmission(player);
            }

            return;
        }

        println("Finished turning in collections.");
        setBotState(BotState.IDLE);
    }

    private int calculateAvailableCollections(String[] collectionItems, int collectionItemCount) {
        int minAvailable = Integer.MAX_VALUE;

        for (String item : collectionItems) {
            int itemCount = Bank.getCount(item);
            println("Found " + itemCount + " of " + item + " in the bank.");
            minAvailable = Math.min(minAvailable, itemCount);
        }

        int collectionsAvailable = minAvailable;
        println("You can complete " + collectionsAvailable + " full collections based on available items.");

        return collectionsAvailable;
    }

    private boolean withdrawMaxCollectionItems(String[] items, int fullCollectionCount) {
        int itemsPerCollection = items.length;
        int maxCollectionsInInventory = 28 / itemsPerCollection;

        int collectionsToWithdraw = Math.min(fullCollectionCount, maxCollectionsInInventory);
        println("Withdrawing " + collectionsToWithdraw + " of each item for the collections.");

        for (String item : items) {  // Loop through each item
            println("Withdrawing " + collectionsToWithdraw + " of " + item);
            if (!Bank.withdraw(item, TransferOptionType.X.getVarbitValue())) {
                println("Failed to withdraw " + collectionsToWithdraw + " of " + item);
                return false;
            }
            Execution.delay(random.nextLong(200, 400));
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

        if (turnInCollections) {
            if (backpackContainsSecondCollectionItems()) {
                handleSecondCollectionSubmission(player);
                return;
            }
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

        if (!summoningArea.contains(player)) {
            println("Moving to summoning area");
            moveTo(summoningArea.getRandomWalkableCoordinate());
        }

        if (Backpack.contains("Ancient remains", ancientRemainsCount) && identifyAncientRemains) {
            Backpack.interact("Ancient remains", "Identify all");
            Execution.delay(random.nextLong(1200, 1800));
        }

        if (turnInCollections) {
            if (backpackContainsSecondCollectionItems()) {
                handleSecondCollectionSubmission(player);
                return;
            }
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
            long currentTime = System.currentTimeMillis();

            if (player.getAnimationId() == ANIMATION_ID_RITUAL) {
                println("Player is already performing the ritual...");
                lastAnimationTime = currentTime;
            } else {
                if (currentTime - lastAnimationTime > RITUAL_TIMEOUT_MS) {
                    println("Ritual not detected for over 5 seconds. Restarting the summoning ritual...");
                    summoningCircle.interact("Summon");
                    Execution.delayUntil(4000, () -> player.getAnimationId() == ANIMATION_ID_RITUAL);
                    lastAnimationTime = currentTime;
                }
            }
        }
    }

    private void handleArchaeology(Player player) {
        handleDialogue();

        if (Backpack.contains("Complete tome", tomeCount) && handInTomes) {
            handInTomesProcedure(player);
        }

        handleAncientRemains();

        if (turnInCollections) {
            if (backpackContainsCollectionItems()) {
                handleCollectionSubmission(player);
                return;
            }

            if (backpackContainsSecondCollectionItems()) {
                handleSecondCollectionSubmission(player);
                return;
            }
        }

        if (Backpack.isFull()) {
            handleFullBackpack(player);
            return;
        }

        if (!excavationArea.contains(player.getCoordinate())) {
            returnToExcavationSite(player);
            return;
        }

        handleChaseSprite(player);
    }

    private void handleDialogue() {
        if (Interfaces.isOpen(1189)) {
            MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77922323);
        }
    }

    private void handInTomesProcedure(Player player) {
        println("Complete tome count reached, handing in tomes");

        stopPlayerAnimation(player);

        teleportToWarsRetreat(player);

        moveToTomeArea(player);

        interactWithDeskToStudyTomes(player);

        teleportBackToEvent();
    }

    private void stopPlayerAnimation(Player player) {
        if (player.getAnimationId() != -1) {
            println("Trying to stop interaction");
            Movement.walkTo(player.getCoordinate().getX(), player.getCoordinate().getY(), false);
            Execution.delayUntil(1200, () -> player.getAnimationId() == -1);
        }
    }

    private void teleportToWarsRetreat(Player player) {
        if (player.getAnimationId() == -1 && ActionBar.containsAbility("War's Retreat Teleport")) {
            if (ActionBar.useAbility("War's Retreat Teleport")) {
                Execution.delayUntil(10000, () -> player.getCoordinate().getRegionId() == 13214);
            }
        }
    }

    private void moveToTomeArea(Player player) {
        if (!tomeArea.contains(player)) {
            println("Moving to tome area");
            moveTo(tomeArea.getRandomWalkableCoordinate());
            println("Moved to tome area");
        }
    }

    private void interactWithDeskToStudyTomes(Player player) {
        if (tomeArea.contains(player)) {
            println("Interacting with desk to study the tome...");
            SceneObject desk = SceneObjectQuery.newQuery().name("Desk").option("Study").results().nearest();
            if (desk != null && desk.interact("Study")) {
                println("Interacted with desk to study");
                Execution.delayUntil(60000, () -> !Backpack.contains("Complete tome"));
            }
            println("All tomes studied.");
        }
    }

    private void teleportBackToEvent() {
        println("Attempting to teleport back to the event...");
        ResultSet<Component> communityComponent = ComponentQuery.newQuery(1431).componentIndex(0).results();
        Component communityButton = communityComponent.first();

        if (communityButton != null && !communityButton.isHidden()) {
            interactWithCommunityComponent(communityButton);
        } else {
            println("Community component not found or not visible.");
        }
    }

    private void interactWithCommunityComponent(Component communityButton) {
        if (MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 4, 93782016)) {
            println("Open the community window");
            Execution.delayUntil(5000, () -> Interfaces.isOpen(1289));
            Execution.delay(random.nextLong(600, 1200));

            ResultSet<Component> teleportComponent = ComponentQuery.newQuery(1289).componentIndex(28).results();
            Component teleportButton = teleportComponent.first();

            if (teleportButton != null && !teleportButton.isHidden()) {
                println("Teleport button found and visible. Interacting...");
                MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 84475932);
                Execution.delay(random.nextLong(1200, 1800));
                println("Teleportation back to the event complete.");
            } else {
                println("Teleport button not found or not visible.");
            }
        }
    }

    private void handleAncientRemains() {
        if (Backpack.contains("Ancient remains", ancientRemainsCount) && identifyAncientRemains) {
            Backpack.interact("Ancient remains", "Identify all");
            Execution.delay(random.nextLong(1200, 1800));

        }
    }

    private void handleFullBackpack(Player player) {
        println("Backpack is full. Moving to bank to load the last preset.");
        if (!bankArea.contains(player.getCoordinate())) {
            println("Player is not in the bank area, moving to bank...");
            Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
            Execution.delay(random.nextLong(6000, 10000));
        } else {
            openAndUseBank();
        }
    }

    private void openAndUseBank() {
        println("Player is in the bank area, opening the bank...");
        Bank.open();
        Execution.delayUntil(10000, Bank::isOpen);
        if (Bank.isOpen()) {
            Execution.delay(random.nextLong(1000, 2000));
            Bank.depositAllExcept("Complete tome");
            Bank.close();
        }
    }

    private void returnToExcavationSite(Player player) {
        println("Returning to excavation site at " + excavationLocation);
        Movement.walkTo(excavationLocation.getX(), excavationLocation.getY(), true);
        Execution.delay(random.nextLong(3000, 5000));
    }

    private void handleChaseSprite(Player player) {
        if (chaseSprite) {
            SpotAnimation archaeologySpot = SpotAnimationQuery.newQuery().animations(7307).results().nearest();
            if (archaeologySpot != null) {
                interactWithSpotAnimation(player, archaeologySpot);
            } else {
                interactWithNearestAncientRemains();
            }
        } else {
            interactWithNearestMysteryRemains(player);
        }
    }

    private void interactWithSpotAnimation(Player player, SpotAnimation archaeologySpot) {
        Coordinate spotCoordinate = archaeologySpot.getCoordinate();
        double distanceToSpot = player.getCoordinate().distanceTo(spotCoordinate);

        if (player.getAnimationId() == 33350 && distanceToSpot <= 1) {
            Execution.delay(random.nextLong(2000, 4000));
            return;
        }

        if (player.getAnimationId() == -1 || distanceToSpot > 1) {
            println("Interacting with 'Mystery remains' at: " + spotCoordinate);
            SceneObject mysteryRemains = SceneObjectQuery.newQuery().name("Mystery remains").option("Excavate").on(spotCoordinate).results().nearest();
            if (mysteryRemains != null) {
                mysteryRemains.interact("Excavate");
                Execution.delay(random.nextLong(3000, 5000));
            }
        }
    }

    private void interactWithNearestAncientRemains() {
        println("No archaeology SpotAnimation found nearby. Interacting with 'Ancient remains'.");
        SceneObject mysteryRemains = SceneObjectQuery.newQuery().name("Mystery remains").option("Excavate").results().nearest();
        if (mysteryRemains != null) {
            mysteryRemains.interact("Excavate");
            Execution.delay(random.nextLong(3000, 5000));
        }
    }

    private void interactWithNearestMysteryRemains(Player player) {
        if (player.getAnimationId() == -1) {
            println("Interacting with the nearest 'Mystery remains'.");
            interactWithNearestAncientRemains();
        } else {
            Execution.delay(random.nextLong(3000, 5000));
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

    private void handleCollectionSubmission(Player player) {
        if (!eepArea.contains(player.getCoordinate())) {
            println("Moving to Eep...");
            Movement.walkTo(eepLocation.getX(), eepLocation.getY(), true);
            Execution.delayUntil(random.nextLong(2000, 3000), player::isMoving);
            Execution.delayUntil(random.nextLong(6000, 10000),() -> !player.isMoving());
        }

        println("Found collection items in backpack. Finding NPC 'Eep'...");
        EntityResultSet<Npc> eepResults = NpcQuery.newQuery().name("Eep").option("Talk to").results();
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
                    println("Collections interface opened.");

                    while (backpackContainsCollectionItems() && Interfaces.isOpen(656)) {
                        ResultSet<Component> contributeAllComponent = ComponentQuery.newQuery(656)
                                .componentIndex(24)
                                .results();

                        Component confirmButton = contributeAllComponent.first();
                        if (confirmButton != null && !confirmButton.isHidden()) {
                            println("Found 'Contribute All' button. Attempting to interact...");
                            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);
                            Execution.delay(random.nextLong(1200, 1800));
                            println("Collection submitted.");
                        } else {
                            println("Failed to find or interact with 'Contribute All' button.");
                            break;
                        }
                    }

                    println("No more collection items in inventory.");
                } else {
                    println("Failed to open collections interface.");
                }
            } else {
                println("NPC interaction failed after 3 attempts.");
            }
        } else {
            println("NPC 'Eep' not found.");
        }
    }

    private void handleSecondCollectionSubmission(Player player) {
        if (!eepArea.contains(player.getCoordinate())) {
            println("Moving to Eep...");
            Movement.walkTo(eepLocation.getX(), eepLocation.getY(), true);
            Execution.delayUntil(random.nextLong(2000, 3000), player::isMoving);
            Execution.delayUntil(random.nextLong(6000, 10000),() -> !player.isMoving());
        }

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
                    println("Collections interface opened, switching to second collection...");

                    Execution.delay(random.nextLong(600, 1200));

                    ResultSet<Component> secondCollectionTab = ComponentQuery.newQuery(656)
                            .componentIndex(30)
                            .results();
                    Component secondTab = secondCollectionTab.first();
                    if (secondTab != null && !secondTab.isHidden()) {
                        println("Second collection tab is visible. Attempting to switch...");
                        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 1, 42991647);
                        Execution.delay(random.nextLong(1200, 1800));

                        while (backpackContainsSecondCollectionItems()) {
                            ResultSet<Component> contributeAllComponent = ComponentQuery.newQuery(656)
                                    .componentIndex(24)
                                    .results();
                            Component confirmButton = contributeAllComponent.first();
                            if (confirmButton != null && !confirmButton.isHidden()) {
                                println("Found 'Contribute All' button. Attempting to confirm...");
                                MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 0, 42991641);
                                Execution.delay(random.nextLong(1200, 1800));
                                println("Second collection submitted.");
                            } else {
                                println("Failed to find or interact with 'Contribute All' button.");
                                break;
                            }
                        }

                        println("No more second collection items in inventory.");
                    } else {
                        println("Failed to find or interact with second collection tab.");
                    }
                } else {
                    println("Failed to open collections interface.");
                }
            } else {
                println("NPC interaction failed after 3 attempts.");
            }
        } else {
            println("NPC 'Eep' not found.");
        }
    }

    private void handleForceCollectionTurnIn(Player player) {
        if(!bankArea.contains(player)){
            Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
            Execution.delay(random.nextLong(6000, 10000));
        }
        if (!Bank.isOpen()) {
            println("Opening the bank...");
            Bank.open();
            Execution.delayUntil(5000, Bank::isOpen);
            Bank.depositAll();
            Execution.delay(random.nextLong(600, 1200));
        }

        String[] firstCollectionItems = {
                "Soiled storybook", "Polished horn", "Dusty snow globe", "Ruined letter", "Strange coins"
        };

        String[] secondCollectionItems = {
                "Ancient skull", "Ancient torso", "Ancient left arm", "Ancient right arm", "Ancient left leg", "Ancient right leg"
        };

        int firstCollectionItemCount = firstCollectionItems.length;
        int secondCollectionItemCount = secondCollectionItems.length;

        println("Calculating available collections from the bank...");
        int firstCollectionAvailable = calculateAvailableCollections(firstCollectionItems, firstCollectionItemCount);
        int secondCollectionAvailable = calculateAvailableCollections(secondCollectionItems, secondCollectionItemCount);

        println("Collections available - First: " + firstCollectionAvailable + ", Second: " + secondCollectionAvailable);

        if (firstCollectionAvailable == 0 && secondCollectionAvailable == 0) {
            println("No collections available to turn in.");
            setBotState(lastActivityState);
            forceCollectionTurnIn = false;
            return;
        }

        int totalFreeSpace = 28;
        while (firstCollectionAvailable > 0 || secondCollectionAvailable > 0) {
            int maxCollectionsForFirst = Math.min(totalFreeSpace / firstCollectionItemCount, firstCollectionAvailable);
            int maxCollectionsForSecond = Math.min(totalFreeSpace / secondCollectionItemCount, secondCollectionAvailable);

            int collectionsToWithdrawFirst = Math.min(maxCollectionsForFirst, totalFreeSpace / firstCollectionItemCount);
            int collectionsToWithdrawSecond = Math.min(maxCollectionsForSecond, (totalFreeSpace - collectionsToWithdrawFirst * firstCollectionItemCount) / secondCollectionItemCount);

            if (collectionsToWithdrawFirst > 0) {
                withdrawMaxCollectionItems(firstCollectionItems, collectionsToWithdrawFirst);
                firstCollectionAvailable -= collectionsToWithdrawFirst;
            }

            if (collectionsToWithdrawSecond > 0) {
                withdrawMaxCollectionItems(secondCollectionItems, collectionsToWithdrawSecond);
                secondCollectionAvailable -= collectionsToWithdrawSecond;
            }

            Bank.close();

            if (collectionsToWithdrawFirst > 0) {
                handleCollectionSubmission(player);
            }

            if (collectionsToWithdrawSecond > 0) {
                handleSecondCollectionSubmission(player);
            }

            if (firstCollectionAvailable == 0 && secondCollectionAvailable == 0) {
                println("All collections turned in.");
                setBotState(lastActivityState);
                forceCollectionTurnIn = false;
                return;
            }

            if(!bankArea.contains(player)){
                Movement.walkTo(bankLocation.getX(), bankLocation.getY(), true);
                Execution.delay(random.nextLong(6000, 10000));
            }
            if (!Bank.isOpen()) {
                Bank.open();
                Execution.delayUntil(5000, Bank::isOpen);
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

    public void setSelectedSkillActionId(int actionId) {
        this.selectedSkillActionId = actionId;
    }

    public int getSelectedSkillActionId() {
        return selectedSkillActionId;
    }

    @Override
    public CoaezEventGraphicsContext getGraphicsContext() {
        return sgc;
    }
}

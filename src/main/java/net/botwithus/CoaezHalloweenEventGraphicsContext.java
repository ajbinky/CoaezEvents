package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;

import static net.botwithus.rs3.script.ScriptConsole.println;

public class CoaezHalloweenEventGraphicsContext extends ScriptGraphicsContext {

    private final CoaezHalloweenEvent coaezHalloweenEvent;

    public CoaezHalloweenEventGraphicsContext(ScriptConsole scriptConsole, CoaezHalloweenEvent script) {
        super(scriptConsole);
        this.coaezHalloweenEvent = script;
        loadConfig();
    }

    public void saveConfig() {
        ScriptConfig config = coaezHalloweenEvent.getConfig();

        if (config != null) {
            config.addProperty("botState", coaezHalloweenEvent.botState.toString());
            config.addProperty("lastActivityState", coaezHalloweenEvent.lastActivityState.toString());
            config.addProperty("identifyAncientRemains", String.valueOf(coaezHalloweenEvent.identifyAncientRemains));
            config.addProperty("chaseSprite", String.valueOf(coaezHalloweenEvent.chaseSprite));
            config.addProperty("ancientRemainsCount", String.valueOf(coaezHalloweenEvent.ancientRemainsCount));
            config.addProperty("thievingDelay", String.valueOf(coaezHalloweenEvent.thievingDelay));
            config.save();
        }
    }

    public void loadConfig() {
        ScriptConfig config = coaezHalloweenEvent.getConfig();

        if (config != null) {
            config.load();

            String thievingDelay = config.getProperty("thievingDelay");
            if (thievingDelay != null) {
                coaezHalloweenEvent.ancientRemainsCount = Integer.parseInt(thievingDelay);
            }

            String ancientRemainsCount = config.getProperty("ancientRemainsCount");
            if (ancientRemainsCount != null) {
                coaezHalloweenEvent.ancientRemainsCount = Integer.parseInt(ancientRemainsCount);
            }

            String botStateValue = config.getProperty("botState");
            if (botStateValue != null) {
                coaezHalloweenEvent.botState = CoaezHalloweenEvent.BotState.valueOf(botStateValue);
            }

            String lastActivityStateValue = config.getProperty("lastActivityState");
            if (lastActivityStateValue != null) {
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.valueOf(lastActivityStateValue);
            }

            String identifyAncientRemainsValue = config.getProperty("identifyAncientRemains");
            if (identifyAncientRemainsValue != null) {
                coaezHalloweenEvent.identifyAncientRemains = Boolean.parseBoolean(identifyAncientRemainsValue);
            }

            String chaseSpriteValue = config.getProperty("chaseSprite");
            if (chaseSpriteValue != null) {
                coaezHalloweenEvent.chaseSprite = Boolean.parseBoolean(chaseSpriteValue);
            }

            println("Script state loaded.");
        }
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("Coaez Halloween Event Settings", ImGuiWindowFlag.AlwaysAutoResize.getValue())) {

            if (ImGui.Button("Archaeology spooky event")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.ARCHAEOLOGY);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.ARCHAEOLOGY;
                coaezHalloweenEvent.getConsole().println("Switched to Archaeology spooky event.");
            }

            if (ImGui.Button("Summoning spooky event")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.SUMMONING);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.SUMMONING;
                coaezHalloweenEvent.getConsole().println("Switched to Summoning spooky event.");
            }

            if (ImGui.Button("Thieving spooky event")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.THIEVING);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.THIEVING;
                coaezHalloweenEvent.getConsole().println("Switched to Thieving spooky event.");
            }
            ImGui.SameLine();
            coaezHalloweenEvent.thievingDelay = ImGui.InputInt("Custom delay for thieving, recommended 5+ seconds", coaezHalloweenEvent.thievingDelay);
            ImGui.Separator();

            ImGui.Text("Applicable to thieving , summoning and archeology");
            coaezHalloweenEvent.identifyAncientRemains = ImGui.Checkbox("Identify ancient remains", coaezHalloweenEvent.identifyAncientRemains);
            ImGui.SameLine();
            ImGui.SetItemWidth(100);
            coaezHalloweenEvent.ancientRemainsCount = ImGui.InputInt("How much remains before we identify", coaezHalloweenEvent.ancientRemainsCount);

            coaezHalloweenEvent.chaseSprite = ImGui.Checkbox("Chase sprite during archeology", coaezHalloweenEvent.chaseSprite);
            ImGui.Separator();

            if (ImGui.Button("Turn in collections")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.TURNINCOLLECTIONS);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.TURNINCOLLECTIONS;
                coaezHalloweenEvent.getConsole().println("Switched to turning in collections event.");
            }
            ImGui.Separator();

            if (ImGui.Button("Pumpkin spooky event")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.PUMPKIN);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.PUMPKIN;
                coaezHalloweenEvent.getConsole().println("Switched to Pumpkin spooky event.");
            }
            ImGui.Text("Only applicable to pumpkin interaction.");
            ImGui.Text("Set high value if you don't want to go on a break, otherwise leave default value which will be randomized every rotation.");
            coaezHalloweenEvent.maxInteractionsBeforePause = ImGui.InputInt("Max interactions before small break", coaezHalloweenEvent.maxInteractionsBeforePause);
            ImGui.Text("How long do we wait after we complete interactions, simulating a small break, random time between the min/max values.");
            coaezHalloweenEvent.minWaitTime = ImGui.InputInt("Min wait time", coaezHalloweenEvent.minWaitTime);
            coaezHalloweenEvent.maxWaitTime = ImGui.InputInt("Max wait time", coaezHalloweenEvent.maxWaitTime);

            ImGui.Separator();

//            if (ImGui.Button("Maze spooky event")) {
//                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.MAZE);
//                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.MAZE;
//                coaezHalloweenEvent.getConsole().println("Switched to Maze spooky event.");
//            }

            saveConfig();

            ImGui.End();
        }
    }

    @Override
    public void drawOverlay() {
    }
}

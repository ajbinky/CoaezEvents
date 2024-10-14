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
    }

    public void saveConfig() {
        ScriptConfig config = coaezHalloweenEvent.getConfig();

        if (config != null) {
            config.addProperty("botState", coaezHalloweenEvent.botState.toString());
            config.addProperty("lastActivityState", coaezHalloweenEvent.lastActivityState.toString());
            config.save();
            println("Script state saved.");
        }
    }

    public void loadConfig() {
        ScriptConfig config = coaezHalloweenEvent.getConfig();

        if (config != null) {
            config.load();

            String botStateValue = config.getProperty("botState");
            if (botStateValue != null) {
                coaezHalloweenEvent.botState = CoaezHalloweenEvent.BotState.valueOf(botStateValue);
            }

            String lastActivityStateValue = config.getProperty("lastActivityState");
            if (lastActivityStateValue != null) {
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.valueOf(lastActivityStateValue);
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

            if (ImGui.Button("Pumpkin spooky event")) {
                coaezHalloweenEvent.setBotState(CoaezHalloweenEvent.BotState.PUMPKIN);
                coaezHalloweenEvent.lastActivityState = CoaezHalloweenEvent.BotState.PUMPKIN;
                coaezHalloweenEvent.getConsole().println("Switched to Pumpkin spooky event.");
            }
            coaezHalloweenEvent.destroyArchBook = ImGui.Checkbox("Destroy archeology book", coaezHalloweenEvent.destroyArchBook);
            coaezHalloweenEvent.identifyAncientRemains = ImGui.Checkbox("Identify ancient remains", coaezHalloweenEvent.identifyAncientRemains);

            ImGui.End();
        }
    }

    @Override
    public void drawOverlay() {
    }
}

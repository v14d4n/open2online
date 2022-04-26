package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;

import java.io.*;
import java.util.StringJoiner;

public class RecreateFirewallRulesScreen extends Screen {
    private final Screen lastScreen;

    public RecreateFirewallRulesScreen(Screen pLastScreen) {
        super(new TranslationTextComponent("gui.opentoonline.recreateFirewallRules"));
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.addButton(new Button(this.width / 2 - 155, this.height / 4 + 120 + 12, 150, 20, new TranslationTextComponent("gui.opentoonline.recreateRules"), (p_96304_) -> {
            recreateFirewallRules();
            this.minecraft.setScreen(lastScreen);
        }));
        this.addButton(new Button(this.width / 2 - 155 + 160, this.height / 4 + 120 + 12, 150, 20, DialogTexts.GUI_CANCEL, (p_96300_) -> {
            this.minecraft.setScreen(lastScreen);
        }));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(MatrixStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        int pX = this.width / 2 - 140;
        int pY = this.height / 4 - 60 + 60;

        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, Math.max(52, height / 4 - 8) - 20, 16777215);
        drawString(pPoseStack, this.font, "Don't use this if everything is working fine!", pX, pY, 16711680);
        drawString(pPoseStack, this.font, "This should only be used when there are no errors", pX, pY + 18, 10526880);
        drawString(pPoseStack, this.font, "publishing the server, but no one can connect to it.", pX, pY + 27, 10526880);
        drawString(pPoseStack, this.font, "Make sure it's necessary.", pX, pY + 36, 10526880);
        drawString(pPoseStack, this.font, "When you click on \"Recreate Rules\" button, the old Minecraft", pX, pY + 54, 10526880);
        drawString(pPoseStack, this.font, "firewall rules will be deleted and new correct ones will be", pX, pY + 63, 10526880);
        drawString(pPoseStack, this.font, "created.", pX, pY + 72, 10526880);
        drawString(pPoseStack, this.font, "This only works on Windows.", pX, pY + 91, 10526880);
        drawString(pPoseStack, this.font, "Recreating firewall rules requires administrator rights.", pX, pY + 100, 10526880);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void recreateFirewallRules() {
        // Get current process path
        String path = JVMUtils.getJVMPath();
        // Final execute command
        StringJoiner exec = new StringJoiner(" && ", "powershell start cmd '/c ", "' -v runAs\"");
        // Delete old rules
        exec.add("netsh advfirewall firewall delete rule name=all program=\"\"\"" + path + "\"\"\"");
        // Create new "in" rule
        exec.add("netsh advfirewall firewall add rule name=\"\"\"Minecraft_in\"\"\" dir=in action=allow program=\"\"\"" + path + "\"\"\"");
        // Create new "out" rule
        exec.add("netsh advfirewall firewall add rule name=\"\"\"Minecraft_out\"\"\" dir=out action=allow program=\"\"\"" + path + "\"\"\"");

        try {
            Runtime.getRuntime().exec(exec.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class JVMUtils {

        private static long getPID() {
            String processName =
                    java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(processName.split("@")[0]);
        }

        public static String getJVMPath() {
            String path = "";
            BufferedReader input = null;
            try {
                File file = File.createTempFile("tempfile",".vbs");
                file.deleteOnExit();
                FileWriter fw = new java.io.FileWriter(file);

                String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                        + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                        + "Set service = locator.ConnectServer()\n"
                        + "Set processes = service.ExecQuery _\n"
                        + " (\"select * from Win32_Process where ProcessId='"
                        + JVMUtils.getPID() +"'\")\n"
                        + "For Each process in processes\n"
                        + "wscript.echo process.ExecutablePath \n"
                        + "Next\n"
                        + "Set WSHShell = Nothing\n";
                fw.write(vbs);
                fw.close();
                Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
                input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                path = input.readLine();
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
            finally {
                try { if (input != null) { input.close(); }  }
                catch (IOException io) { io.printStackTrace(); return null; }
            }
            return path;
        }
    }
}

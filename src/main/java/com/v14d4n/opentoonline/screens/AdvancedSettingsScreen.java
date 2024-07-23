package com.v14d4n.opentoonline.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.v14d4n.opentoonline.server.ModServerOptions;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class AdvancedSettingsScreen extends Screen {

    private static final OptionInstance[] OPTIONS = new OptionInstance[]{ModServerOptions.WHITELIST_MODE, ModServerOptions.ALLOW_PVP};
    private OptionsList optionsList;
    private final Screen lastScreen;

    public AdvancedSettingsScreen(Screen pLastScreen) {
        super(Component.translatable("gui.opentoonline.advancedServerSettings"));
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.addBig(ModServerOptions.LIBRARY);
        // this.optionsList.addBig(ModServerOptions.EDIT_WHITELIST); // TODO: *-*
        this.optionsList.addSmall(OPTIONS);
        this.addWidget(this.optionsList);

        Button.Builder builder = new Button.Builder(CommonComponents.GUI_DONE, (p_96827_) -> {
            this.minecraft.setScreen(this.lastScreen);
        });
        builder.pos(this.width / 2 - 100, this.height - 27);
        builder.size(200, 20);

        this.addRenderableWidget(builder.build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        this.optionsList.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 16777215);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
//        List<FormattedCharSequence> list = tooltipAt(this.optionsList, pMouseX, pMouseY);
//        if (list != null) {
//            this.renderTooltip(pPoseStack, list, pMouseX, pMouseY);
//        }
    }

//    private static List<FormattedCharSequence> tooltipAt(OptionsList p_96288_, int pMouseX, int pMouseY) {
//        Optional<AbstractWidget> optional = p_96288_.getMouseOver(pMouseX, pMouseY);
//
//        if (optional.isPresent()) {
//            AbstractWidget widget = optional.get();
//            try {
//                Field tooltipField = FieldUtils.getDeclaredField(AbstractWidget.class, "tooltip", true); // Works only if you run client from IDE
//                // Field tooltipField = FieldUtils.getDeclaredField(AbstractWidget.class, "f_256816_", true); // Works only in build version
//                Tooltip tooltip = (Tooltip) tooltipField.get(widget);
//                return tooltip != null ? tooltip.toCharSequence(Minecraft.getInstance()) : ImmutableList.of();
//            } catch (IllegalAccessException e) {
//                Logger logger = LogManager.getLogManager().getLogger(OpenToOnline.MOD_ID);
//                logger.log(Level.WARNING, e.getMessage());
//            }
//        }
//
//        return ImmutableList.of();
//    }
}

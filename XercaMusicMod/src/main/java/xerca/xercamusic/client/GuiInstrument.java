package xerca.xercamusic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.lwjgl.glfw.GLFW;
import xerca.xercamusic.common.XercaMusic;
import xerca.xercamusic.common.block.BlockInstrument;
import xerca.xercamusic.common.item.ItemInstrument;
import xerca.xercamusic.common.packets.SingleNotePacket;

import javax.annotation.Nullable;

public class GuiInstrument extends Screen {
    private static final ResourceLocation insGuiTextures = new ResourceLocation(XercaMusic.MODID, "textures/gui/instrument_gui.png");

    private int guiBaseX = 45;
    private int guiBaseY = 80;
    private final boolean[] buttonPushStates;
    private final NoteSound[] noteSounds;
    private static int currentKeyboardOctave = 0;
    private Button octaveUp;
    private Button octaveDown;

    private static final int guiHeight = 201;
    private static final int guiWidth = 401;
    private static final int guiMarginWidth = 7;
    private static final int guiNoteWidth = 8;
    private static final int guiOctaveWidth = guiNoteWidth * 12 + 1;
    private static final int guiOctaveHighlightY = 212;
    private static final int guiOctaveHighlightWidth = 98;
    private static final int guiOctaveHighlightHeight = 92;
    private static final int guiTopKeyboardBottom = 94;
    private static final int guiBottomKeyboardTop = 105;
    private static final int guiOctaveBlockX = 99;
    private static final int guiOctaveBlockY = 212;
    private static final int guiOctaveBlockWidth = 95;
    private static final int guiOctaveBlockHeight = 82;
    private int octaveButtonX;
    private final int octaveButtonY = 30;

    private final Player player;
    private final ItemInstrument instrument;
    private final BlockPos blockInsPos;
    private final MidiHandler midiHandler;

    GuiInstrument(Player player, ItemInstrument instrument, Component title, @Nullable BlockPos blockInsPos) {
        super(title);
        this.player = player;
        this.instrument = instrument;
        this.buttonPushStates = new boolean[ItemInstrument.totalNotes];
        this.noteSounds = new NoteSound[ItemInstrument.totalNotes];
        this.midiHandler = new MidiHandler(this::playSound, this::stopSound);
        this.blockInsPos = blockInsPos;
        if(currentKeyboardOctave < instrument.minOctave) {
            currentKeyboardOctave = instrument.minOctave;
        }
        else if(currentKeyboardOctave > instrument.maxOctave) {
            currentKeyboardOctave = instrument.maxOctave;
        }
        midiHandler.currentOctave = currentKeyboardOctave;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        guiBaseX = (this.width - guiWidth) / 2;
        guiBaseY = (this.height - guiHeight) / 2;
        octaveButtonX = guiBaseX - 10;

        this.octaveUp = this.addRenderableWidget(new Button(octaveButtonX, octaveButtonY, 10, 10,
                new TranslatableComponent("note.upButton"), button -> increaseOctave(),
                (button, poseStack, x, y) -> renderTooltip(poseStack, new TranslatableComponent("ins.octaveTooltip"), x, y)));

        this.octaveDown = this.addRenderableWidget(new Button(octaveButtonX, octaveButtonY + 25, 10, 10,
                new TranslatableComponent("note.downButton"), button -> decreaseOctave(),
                (button, poseStack, x, y) -> renderTooltip(poseStack, new TranslatableComponent("ins.octaveTooltip"), x, y)));
    }

    @Override
    public void tick() {
        super.tick();
        if(blockInsPos != null && minecraft != null ){
            if(player.level.getBlockState(blockInsPos).getBlock() instanceof BlockInstrument blockIns){
                if(blockIns.getItemInstrument() != instrument){
                    minecraft.setScreen(null);
                }
            }
            else{
                minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, insGuiTextures);

        blit(matrixStack, guiBaseX, guiBaseY, this.getBlitOffset(), 0, 0, guiWidth, guiHeight, 512, 512);

        for(int i=0; i<buttonPushStates.length; i++){
            if(buttonPushStates[i]){
                int pushedOctave = i / 12;
                int x = guiBaseX + guiMarginWidth + i*guiNoteWidth + pushedOctave;
                int y = guiBaseY + 11;
                if(pushedOctave > 3){
                    x -= 4 + 48*guiNoteWidth;
                    y = guiBaseY + guiBottomKeyboardTop + 2;
                }
                blit(matrixStack, x, y, this.getBlitOffset(), 402, 11, 7, 82, 512, 512);
            }
        }

        int currentKeyboardOctaveDraw = Math.max(0, currentKeyboardOctave);
        int octaveHighlightX = guiBaseX + guiMarginWidth + currentKeyboardOctaveDraw * guiOctaveWidth - 1;
        int octaveHighlightY = guiBaseY + 3;
        if(currentKeyboardOctave > 3){
            octaveHighlightX -= 4 * guiOctaveWidth;
            octaveHighlightY = guiBaseY + guiBottomKeyboardTop - 6;
        }
        blit(matrixStack, octaveHighlightX, octaveHighlightY, this.getBlitOffset(), 0, guiOctaveHighlightY, guiOctaveHighlightWidth, guiOctaveHighlightHeight, 512, 512);

        for(int i=0; i<8; i++){
            if(i < instrument.minOctave || i > instrument.maxOctave){
                int x = guiBaseX + guiMarginWidth + i*guiOctaveWidth;
                int y = guiBaseY + 11;
                if(i > 3){
                    x -= 4 * guiOctaveWidth;
                    y = guiBaseY + guiBottomKeyboardTop + 2;
                }
                blit(matrixStack, x, y, this.getBlitOffset(), guiOctaveBlockX, guiOctaveBlockY, guiOctaveBlockWidth, guiOctaveBlockHeight, 512, 512);
            }
        }

        drawCenteredString(matrixStack, this.font, "" + (currentKeyboardOctave), octaveButtonX + 4, octaveButtonY + 14, 0xFFFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private int noteIdFromPos(int mouseX, int mouseY) {
        int buttonBaseX = guiBaseX + guiMarginWidth;
        if(mouseX >= buttonBaseX && mouseX <= buttonBaseX + guiWidth - 14
                && mouseY >= guiBaseY + 9 && mouseY <= guiBaseY + guiHeight - 10
                && (mouseY < guiBaseY + guiTopKeyboardBottom || mouseY > guiBaseY + guiBottomKeyboardTop)) {
            int octavePlus = (mouseY < guiBaseY + guiTopKeyboardBottom) ? 0 : 4;
            int octave = octavePlus + (mouseX - buttonBaseX) / (guiOctaveWidth);
            int note = ((mouseX - buttonBaseX) % guiOctaveWidth) / guiNoteWidth;
            if (note < 12) {
                return octave * 12 + note;
            }
        }
        return -1;
    }

    private void playSound(int noteId){
        playSound(new MidiHandler.MidiData(noteId, 0.8f));
    }

    private void playSound(MidiHandler.MidiData data){
        int noteId = data.noteId();

        if(noteId >= 0 && noteId < buttonPushStates.length && !buttonPushStates[noteId]) {
            int note = ItemInstrument.idToNote(noteId);

            ItemInstrument.InsSound noteSound = instrument.getSound(note);
            if (noteSound == null) {
                return;
            }
            noteSounds[noteId] = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientStuff.playNote(noteSound.sound, player.getX(), player.getY(), player.getZ(), data.volume(), noteSound.pitch));
            player.level.addParticle(ParticleTypes.NOTE, player.getX(), player.getY() + 2.2D, player.getZ(), note / 24.0D, 0.0D, 0.0D);
            buttonPushStates[noteId] = true;

            SingleNotePacket pack = new SingleNotePacket(note, instrument, false);
            XercaMusic.NETWORK_HANDLER.sendToServer(pack);
        }
    }

    private void stopSound(int noteId){
        if(noteId >= 0 && noteId < buttonPushStates.length && buttonPushStates[noteId]) {
            if (noteSounds[noteId] != null) {
                noteSounds[noteId].stopSound();
                noteSounds[noteId] = null;
                buttonPushStates[noteId] = false;

                int note = ItemInstrument.idToNote(noteId);
                SingleNotePacket pack = new SingleNotePacket(note, instrument, true);
                XercaMusic.NETWORK_HANDLER.sendToServer(pack);
            }
        }
    }

    private void stopAllSounds(){
        for(int noteId=0; noteId<buttonPushStates.length; noteId++) {
            stopSound(noteId);
        }
    }

    @Override
    public boolean mouseClicked(double dmouseX, double dmouseY, int mouseButton) {
//        XercaMusic.LOGGER.info("Click pos: " + dmouseX);
        int mouseX = (int)Math.round(dmouseX);
        int mouseY = (int)Math.round(dmouseY);

        int noteId = noteIdFromPos(mouseX, mouseY);
        playSound(noteId);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double dmouseX, double dmouseY, int mouseButton) {
        int mouseX = (int)Math.round(dmouseX);
        int mouseY = (int)Math.round(dmouseY);

        int noteId = noteIdFromPos(mouseX, mouseY);
        stopSound(noteId);

        return super.mouseReleased(dmouseX, dmouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double posX, double posY, int mouseButton, double deltaX, double deltaY) {
//        XercaMusic.LOGGER.info("Drag pos: " + posX + " del: " + deltaX);
        int mouseX = (int)Math.round(posX);
        int mouseY = (int)Math.round(posY);
        int prevMouseX = (int)Math.round(posX - deltaX);
        int prevMouseY = (int)Math.round(posY - deltaY);

        int prevNoteId = noteIdFromPos(prevMouseX, prevMouseY);
        int currentNoteId = noteIdFromPos(mouseX, mouseY);
        if(prevNoteId != currentNoteId){
            stopSound(prevNoteId);
            playSound(currentNoteId);
        }

        return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        setFocused(null);
        super.keyPressed(keyCode, scanCode, modifiers);

        if (scanCode >= 16 && scanCode <= 27) {
            int noteId = scanCode - 16 + 12 * Math.max(0, currentKeyboardOctave);
            playSound(noteId);
        }

        if(keyCode == GLFW.GLFW_KEY_A){
            decreaseOctave();
        }
        else if(keyCode == GLFW.GLFW_KEY_S){
            increaseOctave();
        }
        return true;
    }

    private void decreaseOctave() {
        if(currentKeyboardOctave > -3) { // instrument.minOctave){
            currentKeyboardOctave --;
            midiHandler.currentOctave = currentKeyboardOctave;
            stopAllSounds();
        }
    }

    private void increaseOctave() {
        if(currentKeyboardOctave < instrument.maxOctave){
            currentKeyboardOctave ++;
            midiHandler.currentOctave = currentKeyboardOctave;
            stopAllSounds();
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers){
        if (scanCode >= 16 && scanCode <= 27) {
            int noteId = scanCode - 16 + 12 * Math.max(0, currentKeyboardOctave);
            stopSound(noteId);
        }
        return true;
    }

    @Override
    public void removed() {
        midiHandler.closeDevices();
    }
}

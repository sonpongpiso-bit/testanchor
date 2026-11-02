package dev.anchor.macro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

public final class AnchorMacroClient implements ClientModInitializer {
    // Adjust these values if your server needs more time between interactions.
    private static final int PLACE_DELAY_TICKS = 2;
    private static final int CHARGE_DELAY_TICKS = 2;
    private static final int DETONATE_DELAY_TICKS = 2;

    private enum Step { IDLE, WAIT_AFTER_PLACE, WAIT_AFTER_CHARGE, WAIT_BEFORE_DETONATE }

    private Step step = Step.IDLE;
    private boolean wasUseHeld;
    private int ticks;
    private BlockPos anchorPos;
    private int totemHotbarSlot = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.level == null) {
            reset();
            return;
        }

        boolean held = client.options.keyUse.isDown();
        if (!held) {
            wasUseHeld = false;
            if (step != Step.IDLE) reset();
            return;
        }

        // One activation per press/hold. Releasing right-click re-arms the macro.
        if (!wasUseHeld) {
            wasUseHeld = true;
            if (step == Step.IDLE && client.player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
                begin(client);
            }
        }

        if (step == Step.IDLE) return;
        if (--ticks > 0) return;

        switch (step) {
            case WAIT_AFTER_PLACE -> charge(client);
            case WAIT_AFTER_CHARGE -> prepareDetonation(client);
            case WAIT_BEFORE_DETONATE -> detonate(client);
            default -> { }
        }
    }

    private void begin(Minecraft client) {
        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            reset();
            return;
        }

        BlockPos targeted = blockHit.getBlockPos();
        if (client.level.getBlockState(targeted).is(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = targeted;
            charge(client);
            return;
        }

        // Place the anchor using the vanilla client interaction path.
        anchorPos = targeted.relative(blockHit.getDirection());
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
        step = Step.WAIT_AFTER_PLACE;
        ticks = PLACE_DELAY_TICKS;
    }

    private void charge(Minecraft client) {
        if (anchorPos == null || !client.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }

        int glowstoneSlot = findOrSwapIntoHotbar(client, Items.GLOWSTONE);
        if (glowstoneSlot < 0) {
            reset();
            return;
        }

        client.player.getInventory().setSelectedSlot(glowstoneSlot);
        BlockHitResult anchorHit = hitOnAnchor(client);
        if (anchorHit == null) {
            reset();
            return;
        }

        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, anchorHit);
        step = Step.WAIT_AFTER_CHARGE;
        ticks = CHARGE_DELAY_TICKS;
    }

    private void prepareDetonation(Minecraft client) {
        if (anchorPos == null || !client.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)) {
            reset();
            return;
        }

        totemHotbarSlot = findOrSwapIntoHotbar(client, Items.TOTEM_OF_UNDYING);
        if (totemHotbarSlot < 0) {
            reset();
            return;
        }

        client.player.getInventory().setSelectedSlot(totemHotbarSlot);
        step = Step.WAIT_BEFORE_DETONATE;
        ticks = DETONATE_DELAY_TICKS;
    }

    private void detonate(Minecraft client) {
        BlockHitResult anchorHit = hitOnAnchor(client);
        if (anchorHit == null || !client.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            reset();
            return;
        }

        // Vanilla useItemOn sends the ordinary interaction packet; no fabricated packets.
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, anchorHit);
        reset();
    }

    private BlockHitResult hitOnAnchor(Minecraft client) {
        if (anchorPos == null || !client.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)) return null;
        Vec3 center = Vec3.atCenterOf(anchorPos);
        return new BlockHitResult(center, Direction.UP, anchorPos, false);
    }

    /**
     * Finds an item in the hotbar first, then in the main inventory.
     * If found in the main inventory, swaps it into the currently selected hotbar slot
     * through the normal inventory click interaction.
     */
    private int findOrSwapIntoHotbar(Minecraft client, net.minecraft.world.item.Item item) {
        var inventory = client.player.getInventory();

        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).is(item)) return slot;
        }

        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            if (inventory.getItem(inventorySlot).is(item)) {
                int hotbarSlot = inventory.getSelectedSlot();
                // Player inventory menu slots 9-35 are main inventory; 36-44 are hotbar.
                client.gameMode.handleInventoryMouseClick(
                    client.player.inventoryMenu.containerId,
                    inventorySlot,
                    hotbarSlot,
                    ClickType.SWAP,
                    client.player
                );
                return hotbarSlot;
            }
        }
        return -1;
    }

    private void reset() {
        step = Step.IDLE;
        ticks = 0;
        anchorPos = null;
        totemHotbarSlot = -1;
    }
}

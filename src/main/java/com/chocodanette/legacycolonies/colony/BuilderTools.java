package com.chocodanette.legacycolonies.colony;

import com.chocodanette.legacycolonies.LegacyColonies;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.research.util.ResearchConstants;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The builder's tools, read from {@code legacycolonies/builder_tools/<name>.json} (files merged in id order).
 * <p>
 * Each block belongs to the first tool whose block tags hold it. The builder takes the fastest tool of that kind from
 * its inventory: it breaks and places the block that many times faster. Without one it works by hand (speed 1), except
 * for a tool marked {@code required} (the pickaxe for stone): then it asks for one and waits.
 */
public final class BuilderTools extends SimpleJsonResourceReloadListener
{
    public static final BuilderTools INSTANCE = new BuilderTools();

    /**
     * {@code AbstractEntityAIInteract.BLOCK_MINING_DELAY}, and the builder's own half of it.
     */
    private static final double MINING_DELAY = 500 * 0.5;

    /**
     * @param name     a MineColonies equipment type (pickaxe, axe, shovel, hoe…), asked for when the tool is required.
     * @param items    the items of this tool.
     * @param blocks   the blocks worked with it.
     * @param required whether the builder may not work these blocks by hand.
     */
    public record Tool(String name, TagKey<Item> items, List<TagKey<Block>> blocks, boolean required)
    {
        public static final Codec<Tool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.fieldOf("name").forGetter(Tool::name),
          TagKey.hashedCodec(Registries.ITEM).fieldOf("items").forGetter(Tool::items),
          TagKey.hashedCodec(Registries.BLOCK).listOf().fieldOf("blocks").forGetter(Tool::blocks),
          Codec.BOOL.optionalFieldOf("required", false).forGetter(Tool::required)
        ).apply(instance, Tool::new));

        public boolean works(final BlockState state)
        {
            for (final TagKey<Block> tag : blocks)
            {
                if (state.is(tag))
                {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @param items      the tools made of one material.
     * @param multiplier how many times faster they work than a bare hand.
     */
    public record Speed(TagKey<Item> items, double multiplier)
    {
        public static final Codec<Speed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          TagKey.hashedCodec(Registries.ITEM).fieldOf("items").forGetter(Speed::items),
          Codec.DOUBLE.fieldOf("multiplier").forGetter(Speed::multiplier)
        ).apply(instance, Speed::new));
    }

    private record Config(List<Tool> tools, List<Speed> speeds)
    {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Tool.CODEC.listOf().optionalFieldOf("tools", List.of()).forGetter(Config::tools),
          Speed.CODEC.listOf().optionalFieldOf("speeds", List.of()).forGetter(Config::speeds)
        ).apply(instance, Config::new));
    }

    /**
     * The state of the block the builder placed last, set by {@code StructurePlacerMixin} and read when the builder
     * waits after placing it.
     */
    public static final ThreadLocal<BlockState> LAST_PLACED = new ThreadLocal<>();

    private List<Tool>  tools  = List.of();
    private List<Speed> speeds = List.of();

    private BuilderTools()
    {
        super(new Gson(), LegacyColonies.MOD_ID + "/builder_tools");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resourceManager, final ProfilerFiller profiler)
    {
        final List<Tool> loadedTools = new ArrayList<>();
        final List<Speed> loadedSpeeds = new ArrayList<>();
        new TreeMap<>(files).forEach((file, json) -> Config.CODEC.parse(JsonOps.INSTANCE, json)
          .resultOrPartial(error -> LegacyColonies.LOGGER.error("Invalid builder tools {}: {}", file, error))
          .ifPresent(config -> {
              loadedTools.addAll(config.tools());
              loadedSpeeds.addAll(config.speeds());
          }));
        tools = loadedTools;
        speeds = loadedSpeeds;
        LegacyColonies.LOGGER.info("Loaded {} builder tools and {} tool speeds", loadedTools.size(), loadedSpeeds.size());
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event)
    {
        event.addListener(INSTANCE);
    }

    /**
     * The tool that works this block, if any.
     */
    @Nullable
    public static Tool toolFor(final BlockState state)
    {
        for (final Tool tool : INSTANCE.tools)
        {
            if (tool.works(state))
            {
                return tool;
            }
        }
        return null;
    }

    /**
     * How many times faster than a bare hand this item works (1 for an item of no listed material).
     */
    public static double multiplier(final ItemStack stack)
    {
        for (final Speed speed : INSTANCE.speeds)
        {
            if (stack.is(speed.items()))
            {
                return speed.multiplier();
            }
        }
        return 1;
    }

    /**
     * The inventory slot of the fastest tool of this kind, or -1.
     */
    public static int bestSlot(final InventoryCitizen inventory, final Tool tool)
    {
        int best = -1;
        double bestMultiplier = 0;
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(tool.items()) && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage()))
            {
                final double multiplier = multiplier(stack);
                if (multiplier > bestMultiplier)
                {
                    best = i;
                    bestMultiplier = multiplier;
                }
            }
        }
        return best;
    }

    /**
     * How many times faster the builder works this block with the tools it has.
     */
    public static double speed(final InventoryCitizen inventory, final BlockState state)
    {
        final Tool tool = toolFor(state);
        if (tool == null)
        {
            return 1;
        }
        final int slot = bestSlot(inventory, tool);
        return slot < 0 ? 1 : multiplier(inventory.getStackInSlot(slot));
    }

    /**
     * Whether the builder may not work this block, as it needs a tool it does not have.
     */
    public static boolean lacksRequiredTool(final InventoryCitizen inventory, final BlockState state)
    {
        final Tool tool = toolFor(state);
        return tool != null && tool.required() && bestSlot(inventory, tool) < 0;
    }

    /**
     * Replaces {@code AbstractEntityAIBasic.holdEfficientTool} for the builder: holds the fastest tool for the block,
     * or nothing. Without a required tool it asks for one and returns false, so the block waits.
     */
    public static boolean holdTool(final EntityAIStructureBuilder ai, final BlockState state)
    {
        final AbstractEntityCitizen worker = ai.getWorker();
        final Tool tool = toolFor(state);
        final int slot = tool == null ? -1 : bestSlot(worker.getInventoryCitizen(), tool);
        if (slot >= 0)
        {
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, slot);
            return true;
        }
        if (tool != null && tool.required())
        {
            requestTool(ai, tool);
            return false;
        }
        worker.getCitizenData().setJobStatus(JobStatus.WORKING);
        CitizenItemUtils.removeHeldItem(worker);
        return true;
    }

    /**
     * Asks for the tool through MineColonies (once: an open request is not repeated) and marks the builder stuck.
     */
    public static void requestTool(final EntityAIStructureBuilder ai, final Tool tool)
    {
        final EquipmentTypeEntry type = equipmentType(tool.name());
        if (type != null)
        {
            ai.checkForToolOrWeapon(type);
        }
        else
        {
            ai.getWorker().getCitizenData().setJobStatus(JobStatus.STUCK);
        }
    }

    @Nullable
    private static EquipmentTypeEntry equipmentType(final String name)
    {
        return switch (name)
        {
            case "pickaxe" -> ModEquipmentTypes.pickaxe.get();
            case "axe" -> ModEquipmentTypes.axe.get();
            case "shovel" -> ModEquipmentTypes.shovel.get();
            case "hoe" -> ModEquipmentTypes.hoe.get();
            default -> null;
        };
    }

    /**
     * Replaces {@code getBlockMiningTime} for the builder: MineColonies' delay with a bare hand, divided by the speed of
     * the tool in hand (MineColonies itself divides by the tool's own mining speed).
     */
    public static int miningTime(final EntityAIStructureBuilder ai, final BlockState state, final BlockPos pos)
    {
        final AbstractEntityCitizen worker = ai.getWorker();
        final double reduction = 1.0 - worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects()
          .getEffectStrength(ResearchConstants.BLOCK_BREAK_SPEED);
        final ItemStack held = worker.getMainHandItem();
        final Tool tool = toolFor(state);
        final double multiplier = tool != null && held.is(tool.items()) ? multiplier(held) : 1;
        return (int) (MINING_DELAY * Math.pow(0.85, ai.getBreakSpeedLevel() / 2.0) * state.getDestroySpeed(worker.level(), pos) * reduction
                        / multiplier);
    }
}

package us.dison.compactmachines 
 
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Material;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import team.reborn.energy.api.EnergyStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import us.dison.compactmachines.block.MachineBlock;
import us.dison.compactmachines.block.MachineWallBlock;
import us.dison.compactmachines.block.TunnelWallBlock;
import us.dison.compactmachines.block.entity.MachineWallBlockEntity;
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity;
import us.dison.compactmachines.enums.MachineSize;
import us.dison.compactmachines.block.entity.MachineBlockEntity;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.data.persistent.tunnel.Tunnel;
import us.dison.compactmachines.data.persistent.tunnel.TunnelType;
import us.dison.compactmachines.item.PSDItem;
import us.dison.compactmachines.item.TunnelItem;
import net.minecraft.tag.TagKey
import java.util.function._
import net.minecraft.block.entity.BlockEntity
import us.dison.compactmachines.crafting.projector.ProjectBlock.apply
import us.dison.compactmachines.crafting.projector.ProjectBlock

object CompactMachines extends ModInitializer: 
  val MODID = "compactmachines" 
  val LOGGER = LogManager.getLogger("CompactMachines")
  val WRENCH_TAG = TagKey.of(Registry.ITEM_KEY, Identifier("c", "wrenches"))
  // Biome & biome key 
  private val CMBIOME = createCMBiome() 
  val CMBIOME_KEY = RegistryKey.of(Registry.BIOME_KEY, Identifier(MODID, "compactmachines")).nn

  // World key 
  val CMWORLD_KEY = RegistryKey.of(Registry.WORLD_KEY, Identifier(MODID, "compactmachinesdim")).nn 
  // Item/Block ID's 
  val ID_TINY = Identifier(MODID, "machine_tiny")
  val ID_SMALL = Identifier(MODID, "machine_small") 
  val ID_NORMAL = Identifier(MODID, "machine_normal")
  val ID_LARGE = Identifier(MODID, "machine_large")
  val ID_GIANT = Identifier(MODID, "machine_giant")
  val ID_MAXIMUM = Identifier(MODID, "machine_maximum") 
  val ID_WALL_UNBREAKABLE = Identifier(MODID, "solid_wall")
  val ID_WALL = Identifier(MODID, "wall")
  val ID_WALL_TUNNEL = Identifier(MODID, "tunnel_wall")
  val ID_PSD = Identifier(MODID, "personal_shrinking_device") 
  val ID_REDSTONE_TUNNEL = Identifier(MODID, "redstone_tunnel") 
  val ID_TUNNEL = Identifier(MODID, "tunnel") 
  val ID_PROJECTOR = Identifier(MODID, "projector")
  // Block settings 
  val SETTINGS_BLOCK_MACHINE = FabricBlockSettings.of(Material.METAL).nn.strength(4.0f).nn.requiresTool();
  val SETTINGS_BLOCK_WALL = FabricBlockSettings.of(Material.METAL).nn.strength(4.0f).nn.requiresTool();
  // Block 
  val BLOCK_MACHINE_TINY = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Tiny))
  val BLOCK_MACHINE_SMALL = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Small))
  val BLOCK_MACHINE_NORMAL = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Normal))
  val BLOCK_MACHINE_LARGE = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Large))
  val BLOCK_MACHINE_GIANT = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Giant))
  val BLOCK_MACHINE_MAXIMUM = MachineBlock(SETTINGS_BLOCK_MACHINE, Some(MachineSize.Maximum))
  val BLOCK_WALL_UNBREAKABLE = MachineWallBlock(SETTINGS_BLOCK_WALL, false) 
  val BLOCK_WALL = MachineWallBlock(SETTINGS_BLOCK_WALL, true) 
  val BLOCK_WALL_TUNNEL = TunnelWallBlock(SETTINGS_BLOCK_WALL, false) 
  val BLOCK_PROJECTOR = ProjectBlock(SETTINGS_BLOCK_WALL) 

  val SETTINGS_ITEM = FabricItemSettings() 
  
  val ITEM_ITEM_TUNNEL = TunnelItem(SETTINGS_ITEM, Some(TunnelType.Normal)) 
  val ITEM_REDSTONE_TUNNEL = TunnelItem(SETTINGS_ITEM, Some(TunnelType.Redstone)) 
  val ITEM_PSD = PSDItem(FabricItemSettings().maxCount(1)) 
  val ITEM_MACHINE_TINY = BlockItem(BLOCK_MACHINE_TINY, SETTINGS_ITEM)
  val ITEM_MACHINE_SMALL = BlockItem(BLOCK_MACHINE_SMALL, SETTINGS_ITEM)
  val ITEM_MACHINE_NORMAL = BlockItem(BLOCK_MACHINE_NORMAL, SETTINGS_ITEM)
  val ITEM_MACHINE_LARGE = BlockItem(BLOCK_MACHINE_LARGE, SETTINGS_ITEM)
  val ITEM_MACHINE_GIANT = BlockItem(BLOCK_MACHINE_GIANT, SETTINGS_ITEM)
  val ITEM_MACHINE_MAXIMUM = BlockItem(BLOCK_MACHINE_MAXIMUM, SETTINGS_ITEM)
  val ITEM_WALL_UNBREAKABLE = BlockItem(BLOCK_WALL_UNBREAKABLE, SETTINGS_ITEM) 
  val ITEM_WALL = BlockItem(BLOCK_WALL, SETTINGS_ITEM) 
  // val ITEM_WALL_TUNNEL = BlockItem(BLOCK_WALL_TUNNEL, SETTINGS_ITEM)
  val ITEM_PROJECTOR = BlockItem(BLOCK_PROJECTOR, SETTINGS_ITEM)
  val CM_ITEMGROUP = FabricItemGroupBuilder.create(Identifier(MODID, "title")).nn
    .icon(() => ItemStack(BLOCK_MACHINE_NORMAL)).nn
    .appendItems((badStacks : java.util.List[ItemStack]) => 
        Option(badStacks).foreach(stacks =>  
          stacks.add(ItemStack(ITEM_PSD)) 
          stacks.add(ItemStack(ITEM_MACHINE_TINY))
          stacks.add(ItemStack(ITEM_MACHINE_SMALL))
          stacks.add(ItemStack(ITEM_MACHINE_NORMAL))
          stacks.add(ItemStack(ITEM_MACHINE_LARGE))
          stacks.add(ItemStack(ITEM_MACHINE_GIANT))
          stacks.add(ItemStack(ITEM_MACHINE_MAXIMUM))
          stacks.add(ItemStack(ITEM_WALL_UNBREAKABLE))
          stacks.add(ItemStack(ITEM_WALL))
          stacks.add(ItemStack(ITEM_PROJECTOR))
          // omitting wall tunnel on purpose 
          val redstoneStack = ItemStack(ITEM_REDSTONE_TUNNEL) 
          // redstoneStack.setSubNbt("type", NbtString.of(TunnelType.Redstone.tunnelName))
          val normalStack = ItemStack(ITEM_ITEM_TUNNEL) 
          // normalStack.setSubNbt("type", NbtString.of(TunnelType.Normal.tunnelName)) 
          stacks.add(redstoneStack)
          stacks.add(normalStack)
      )
  ).build()
  lazy val MACHINE_BLOCK_ENTITY : BlockEntityType[MachineBlockEntity] = Registry.register(Registry.BLOCK_ENTITY_TYPE, MODID + ":machine_block_entity", FabricBlockEntityTypeBuilder.create( 
    (a: BlockPos | Null, b: BlockState | Null) => MachineBlockEntity(a.nn, b.nn, None, None), 
    BLOCK_MACHINE_TINY, 
    BLOCK_MACHINE_SMALL, 
    BLOCK_MACHINE_NORMAL,
    BLOCK_MACHINE_LARGE,
    BLOCK_MACHINE_GIANT,
    BLOCK_MACHINE_MAXIMUM).build(null)).nn
  lazy val TUNNEL_WALL_BLOCK_ENTITY : BlockEntityType[TunnelWallBlockEntity] = Registry.register(Registry.BLOCK_ENTITY_TYPE, MODID + ":tunnel_wall_block_entity", FabricBlockEntityTypeBuilder.create(TunnelWallBlockEntity.apply(_, _), BLOCK_WALL_TUNNEL).build(null)).nn 
  lazy val MACHINE_WALL_BLOCK_ENTITY : BlockEntityType[MachineWallBlockEntity] = Registry.register(Registry.BLOCK_ENTITY_TYPE, MODID + ":machine_wall_block_entity", FabricBlockEntityTypeBuilder.create(MachineWallBlockEntity.apply(_, _), BLOCK_WALL_UNBREAKABLE).build(null)).nn
  private var roomManagerVar : Option[RoomManager] = Option.empty
  // safe, it shouldn't be accessed before server spins up 
  def roomManager = roomManagerVar.get 

  override def onInitialize(): Unit = 
    ServerWorldEvents.LOAD.register((server, world) => 
        if world.getRegistryKey().equals(CMWORLD_KEY)  && roomManagerVar.isDefined then 
          roomManager.onServerWorldLoad(world) 
    )
    ServerTickEvents.START_WORLD_TICK.register(world =>
        if world.getRegistryKey().equals(CMWORLD_KEY) && roomManagerVar.isDefined then  
          roomManager.onServerWorldTick(world)
    )
    ServerLifecycleEvents.SERVER_STARTED.register(server =>
        val world = server.getWorld(CMWORLD_KEY) 
        if world == null then 
          LOGGER.warn("couldn't start room manager") 
        else 
          roomManagerVar = Some(RoomManager.get(world))
    )
    
    Registry.register(BuiltinRegistries.BIOME, CMBIOME_KEY.getValue(), CMBIOME)
   
    MACHINE_BLOCK_ENTITY 
    MACHINE_WALL_BLOCK_ENTITY
    TUNNEL_WALL_BLOCK_ENTITY
    def internalHelper[T](connectedSetter: (TunnelWallBlockEntity, Boolean) => Unit, targetGetter: TunnelWallBlockEntity => Option[T], machineEntity : MachineBlockEntity, direction: Direction | Null):T | Null  =
      LOGGER.info(machineEntity.machineID)
      machineEntity.machineID.flatMap(roomManager.getRoomByNumber(_)).flatMap(room =>
        LOGGER.info("Is a valid room")
        room.tunnels.filter(t => t.tunnelType == TunnelType.Normal && t.face.toDirection() == Option(direction)).collectFirst(scala.Function.unlift(tunnel =>
          if (machineEntity.getWorld().isClient) {
            LOGGER.warn("Not a server")
            None 
          } else {
          machineEntity.getWorld().getServer().getWorld(CMWORLD_KEY).nn.getBlockEntity(tunnel.pos) match 
            case wall : TunnelWallBlockEntity => 
              connectedSetter(wall, false) 
              targetGetter(wall) match 
                case Some(intlTarget) => 
                  connectedSetter(wall, true) 
                  Some(intlTarget)
                case None =>  
                  LOGGER.warn("No internal target")
                  None
            case _ => 
              LOGGER.warn("Tunnel pos isn't a tunnel")
              None
          }
        ))
      ).orNull
    ItemStorage.SIDED.registerForBlockEntity[MachineBlockEntity](
      (machineEntity, direction) => 
        internalHelper(_.connectedToItem = _, _.intlItemTarget, machineEntity, direction)
    , MACHINE_BLOCK_ENTITY)
    FluidStorage.SIDED.registerForBlockEntity[MachineBlockEntity](
      (machineEntity, direction) => 
        internalHelper(_.connectedToFluid = _, _.intlFluidTarget, machineEntity, direction)
    , MACHINE_BLOCK_ENTITY)
    EnergyStorage.SIDED.registerForBlockEntity[MachineBlockEntity](
      (machineEntity, direction) => 
        internalHelper(_.connectedToEnergy = _, _.intlEnergyTarget, machineEntity, direction)
    , MACHINE_BLOCK_ENTITY)
    ItemStorage.SIDED.registerForBlockEntity[TunnelWallBlockEntity](
      (wall, direction) => 
        wall.extItemTarget.orNull 
      , TUNNEL_WALL_BLOCK_ENTITY)
    FluidStorage.SIDED.registerForBlockEntity[TunnelWallBlockEntity](
      (wall, direction) => 
        wall.extFluidTarget.orNull 
      , TUNNEL_WALL_BLOCK_ENTITY)
    EnergyStorage.SIDED.registerForBlockEntity[TunnelWallBlockEntity](
      (wall, direction) => 
        wall.extEnergyTarget.orNull 
      , TUNNEL_WALL_BLOCK_ENTITY)
    Registry.register(Registry.BLOCK, ID_TINY, BLOCK_MACHINE_TINY)
    Registry.register(Registry.BLOCK, ID_SMALL, BLOCK_MACHINE_SMALL)
    Registry.register(Registry.BLOCK, ID_NORMAL, BLOCK_MACHINE_NORMAL)
    Registry.register(Registry.BLOCK, ID_LARGE, BLOCK_MACHINE_LARGE)
    Registry.register(Registry.BLOCK, ID_GIANT, BLOCK_MACHINE_GIANT)
    Registry.register(Registry.BLOCK, ID_MAXIMUM, BLOCK_MACHINE_MAXIMUM)
    Registry.register(Registry.BLOCK, ID_WALL_UNBREAKABLE, BLOCK_WALL_UNBREAKABLE)
    Registry.register(Registry.BLOCK, ID_WALL, BLOCK_WALL)
    Registry.register(Registry.BLOCK, ID_WALL_TUNNEL, BLOCK_WALL_TUNNEL)
    Registry.register(Registry.BLOCK, ID_PROJECTOR, BLOCK_PROJECTOR)

    Registry.register(Registry.ITEM, ID_TINY, ITEM_MACHINE_TINY) 
    Registry.register(Registry.ITEM, ID_SMALL, ITEM_MACHINE_SMALL) 
    Registry.register(Registry.ITEM, ID_NORMAL, ITEM_MACHINE_NORMAL) 
    Registry.register(Registry.ITEM, ID_LARGE, ITEM_MACHINE_LARGE) 
    Registry.register(Registry.ITEM, ID_GIANT, ITEM_MACHINE_GIANT) 
    Registry.register(Registry.ITEM, ID_MAXIMUM, ITEM_MACHINE_MAXIMUM) 
    Registry.register(Registry.ITEM, ID_WALL_UNBREAKABLE, ITEM_WALL_UNBREAKABLE) 
    Registry.register(Registry.ITEM, ID_WALL, ITEM_WALL) 
    // Registry.register(Registry.ITEM, ID_WALL_TUNNEL, ITEM_WALL_TUNNEL) 
    Registry.register(Registry.ITEM, ID_PROJECTOR, ITEM_PROJECTOR)
    Registry.register(Registry.ITEM, ID_TUNNEL, ITEM_ITEM_TUNNEL) 
    Registry.register(Registry.ITEM, ID_REDSTONE_TUNNEL, ITEM_REDSTONE_TUNNEL) 
    Registry.register(Registry.ITEM, ID_PSD, ITEM_PSD)
    
    LOGGER.info("CompactMachines initialized")
  def createCMBiome() = 
    val spawnSettings = SpawnSettings.Builder() 
    val generationSettings = GenerationSettings.Builder() 

    Biome.Builder()
      .precipitation(Biome.Precipitation.NONE)
      .category(Biome.Category.NONE) 
      .temperature(0.8f)
      .downfall(0)
      .effects(BiomeEffects.Builder()
        .waterColor(0x3f76e4)
        .waterFogColor(0x050533)
        .fogColor(0xc0d8ff)
        .skyColor(0x77adff)
        .build()
      ).spawnSettings(spawnSettings.build())
      .generationSettings(generationSettings.build())
      .build()
          
    
  


package us.dison.compactmachines.util

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.block.entity.MachineWallBlockEntity;
import us.dison.compactmachines.enums.MachineSize;
import us.dison.compactmachines.data.persistent.RoomManager;

object RoomUtil: 
  def getCenterPosByID(id: Int): BlockPos = 
    val pos = BlockPos.Mutable(255+8, 7, 255+8) 
    var direction = Direction.NORTH 

    var index = 1 
    // inclusive
    for i <- 2 to id do 
      if index != id then 
        val curLegLength : Int = i / 2
        for legIndex <- 0 until curLegLength do 

          pos.move(direction, 16) 
          index += 1  
        end for 
        direction = direction.rotateCounterclockwise(Direction.Axis.Y)
    end for 
    return pos.toImmutable()

  def nextID(roomManager: RoomManager): Int = 
    var i = 1 
    while roomManager.existsRoomByNumber(i) do 
      i += 1
    return i

  def setCube(world: World, box: Box, block: BlockState) = 
    val corner1 = BlockPos(box.maxX, box.maxY, box.maxZ)
    val corner2 = BlockPos(box.minX, box.minY, box.minZ) 

    List(corner1, corner2).foreach(blockPos => 
        if !world.canSetBlock(blockPos) then 
          CompactMachines.LOGGER.error("Can't set block! " + blockPos.toShortString())
        else 
          world.setBlockState(blockPos, block))
  def setWallNBT(world: World, box: Box, id: Int) = 
    val corner1 = BlockPos(box.maxX, box.maxY, box.maxZ)
    val corner2 = BlockPos(box.minX, box.minY, box.minZ)

    List(corner1, corner2).foreach(blockPos => 
        val blockEntity = world.getBlockEntity(blockPos) 
        if blockEntity.isInstanceOf[MachineWallBlockEntity] then 
          blockEntity.nn.asInstanceOf[MachineWallBlockEntity].setParentID(id)
    )
  def getBox(centerPos : BlockPos, size: Int) = 
    val s : Int = size / 2 + 1
    val corner1 = centerPos.add(s, s, s).nn 
    val corner2 = centerPos.add(-s, -s, -s).nn
    Box(corner1, corner2)
  def generateRoomFromId(world: World, id: Int, machineSize: MachineSize) =
    generateRoom(world, getCenterPosByID(id), machineSize, id)
  def generateRoom(world: World, centerPos: BlockPos, machineSize: MachineSize, id: Int) = 
    if world.isClient() then 
      () 
    else 
      CompactMachines.LOGGER.info("Generating a new room #" + id + " of size" + machineSize.toString + " at " + centerPos.toShortString)

      val chunkPos = ChunkPos(centerPos) 
      world.asInstanceOf[ServerWorld].setChunkForced(chunkPos.x, chunkPos.z, true) 
      val box = getBox(centerPos, machineSize.size)
      setCube(world, 
              box, 
              CompactMachines.BLOCK_WALL_UNBREAKABLE.getDefaultState(), 
      )
      val inner = getBox(centerPos, machineSize.size - 2) 
      setCube(world, 
             inner,
              Blocks.AIR.getDefaultState
      )
      setWallNBT(world,
                box, 
                id 
      ) 

      CompactMachines.LOGGER.info("Done generating room #" + id.toString)


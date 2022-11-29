package us.dison.compactmachines.item 

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import net.minecraft.item.Item.Settings


class TunnelItem(settings: Settings, val tunnelType : Option[TunnelType]) extends Item(settings):
  override def getDefaultStack: ItemStack | Null =
    val s = super.getDefaultStack 
    tunnelType match 
      case Some(t) => 
        // todo this is suspicious
        // s.setSubNbt("type", NbtString.of(t.tunnelName))
        s
      case _ => s
  override def getName(stack: ItemStack): Text = 
    lazy val defName = TranslatableText("item.compactmachines.tunnels.tunnel")
    stack.getNbt match 
      case null => defName 
      case stackNbt => 
        stackNbt.get("type") match 
          case null => defName 
          case typeNbt => 
            TunnelType.byName(typeNbt.asString()).map((tt: TunnelType) => TranslatableText(s"item.compactmachines.tunnels.${tt.tunnelName}")).getOrElse(defName)
    

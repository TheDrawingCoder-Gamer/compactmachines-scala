package us.dison.compactmachines.enums 

enum MachineSize(val name: String, val size: Int) {
  case Tiny extends MachineSize("tiny", 3)
  case Small extends MachineSize("small", 5)
  case Normal extends MachineSize("normal", 7)
  case Large extends MachineSize("large", 9)
  case Giant extends MachineSize("giant", 11)
  case Maximum extends MachineSize("maximum", 13)
}
object MachineSize {
  def getFromSize(str: String): Option[MachineSize] =
    str match
      case "tiny" => Some(Tiny)
      case "small" => Some(Small)
      case "normal" => Some(Normal)
      case "large" => Some(Large)
      case "giant" => Some(Giant)
      case "maximum" => Some(Maximum)
      case _ => None
}


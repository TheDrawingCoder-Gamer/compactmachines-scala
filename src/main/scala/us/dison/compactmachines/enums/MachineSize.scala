package us.dison.compactmachines.enums 

enum MachineSize(val name: String, val size: Int): 
  case Tiny extends MachineSize("tiny", 3) 
  case Small extends MachineSize("small", 5)
  case Normal extends MachineSize("normal", 7)
  case Large extends MachineSize("large", 9)
  case Giant extends MachineSize("giant", 11)
  case Maximum extends MachineSize("maximum", 13)
object MachineSize: 
  def getFromSize(str: String): Option[MachineSize] = 
    str match 
      case "tiny" => Option(Tiny) 
      case "small" => Option(Small) 
      case "normal" => Option(Normal)
      case "large" => Option(Large) 
      case "giant" => Option(Giant)
      case "maximum" => Option(Maximum) 
      case _ => Option.empty

  

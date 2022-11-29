package us.dison.compactmachines.util

class Lazy[+T](supplier : => T) {
  lazy val value: T = supplier
}

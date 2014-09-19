package models

import sorm._

class DB extends Instance(entities = Set(Entity[Person]()), url = "jdbc:h2:mem:test")

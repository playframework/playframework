
Since a Slick query can be manipulated just as a normal Scala collection you will typically will not have the same need for DAO objects as with your run-of-the-mill ORM. Most of the time, it can be fine to simply run the query right there in your application. There are 2 issues with this though: your application logic and persistence layer will be more tied together and you will have a low reuse of, possibly complex, queries and, even worse, complex operation such as joining, updating, etc etc.

It therefore adviced to add traits that represents the different joins. 

To illustrate this, imagine you have two tables called Decks and Details. A deck has details so you will typically have to extract details from decks at different places in your application. 

You can create multiple traits as follow that contain the logic you need to do joins. 


```scala
//deck.scala:
private trait DecksDetails {
  def getDetails(thisDeck: Deck) = for {
      deck <- Deck if deck.id === thisDeck.id
      detail <- Details if detail.id === deck.id
  } yield {
      detail
  }
  //....
}

object Decks extends Table[Deck]("DECK") with DecksDetails { 
}

//detail.scala
object Details extends Table[Detail]("DETAILS") { 
}
```

This way it will be easy to discover to find queries and to keep the code DRY.



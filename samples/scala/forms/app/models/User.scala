package models

case class User(
  username: String, 
  password: String,
  email: String,
  profile: UserProfile
)

case class UserProfile(
  country: String,
  address: Option[String],
  age: Option[Int]
)
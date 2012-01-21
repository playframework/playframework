package models

case class Contact(
  firstname: String,
  lastname: String,
  company: Option[String],
  informations: Seq[ContactInformation]
)

case class ContactInformation(
  label: String,
  email: Option[String],
  phones: List[String]
)
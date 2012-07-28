package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class JCustomer extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@Email
	@Required
	public String email;

	@OneToMany
	@Valid
	public List<JOrder> orders;

}

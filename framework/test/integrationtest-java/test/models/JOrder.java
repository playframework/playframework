package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import play.data.format.Formats.DateTime;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class JOrder extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@ManyToOne
	public JCustomer customer;

	@Required
	@DateTime(pattern = "yyyy-MM-dd")
	public DateTime date;

	@OneToMany
	@Valid
	public List<JOrderItem> items;

}

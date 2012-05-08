package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.format.Formats.DateTime;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class JOrderItem extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@ManyToOne
	public JOrder order;

	@Required
	@Min(1)
	public Integer qty;

	@Required
	public String productCode;

	@DateTime(pattern = "yyyy-MM-dd")
	public DateTime deliveryDate;
}

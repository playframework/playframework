package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.format.Formats.DateTime;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Pattern;
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
	@Pattern(value = "[A-Z]{4}-[0-9]{3,}", message = "A valid product code is required")
	public String productCode;

	@DateTime(pattern = "yyyy-MM-dd")
	public Date deliveryDate;
}

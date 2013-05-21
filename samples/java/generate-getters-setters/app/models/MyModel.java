package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;


import com.avaje.ebean.*;

public class MyModel extends Model {

	public String firstName;
	public String lastName;
	public Integer age;

	public String getFirstName() {

		System.out.println("inside getFirstName()");

		return "getFirstName()" + firstName;
	}

	public void setLastName(String lastName) {

		System.out.println("inside setLastName()");

		this.lastName = "setLastName()" + lastName;
	}

}


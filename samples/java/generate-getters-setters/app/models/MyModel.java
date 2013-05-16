package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;


import com.avaje.ebean.*;


public class MyModel extends Model {

	public String gender;
	public Integer age;

	public Integer getAge() {

		// A lady never ages past 29
		return 29;
	}

}


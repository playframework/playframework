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
	public String lifeStory;

	public Integer getAge() {

		// A lady never ages past 29
		return 29;
	}

	public void setLifeStory(String lifeStory) {

		if(lifeStory == null) {
			this.lifeStory = null;
			return;
		}

		// Don't have all day, give us the short version
		if(lifeStory.length() > 15)
			this.lifeStory = lifeStory.substring(0, 15) + "...";

	}

}


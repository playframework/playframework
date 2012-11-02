package play.filters;

import play.*;
import play.libs.*;
import play.mvc.*;
import play.mvc.Http.*;

public interface Filter{
	public Result call(F.Function<Context, Result> next, Context context) throws Throwable;
}
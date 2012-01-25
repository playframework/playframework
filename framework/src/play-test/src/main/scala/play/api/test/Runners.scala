package play.api.test

import org.specs2.specification.SpecificationStructure

/**
 * Specs runner based on Specs2
 */
object SpecRunner{
   def main(args: Array[String]) {
        specs2.run(args.map{specName => 
        	try {
	        	Class.forName(specName).newInstance().asInstanceOf[SpecificationStructure]
	    	} catch {
		    	case e:Exception=> 
		    	println("could not create a specs2 specification for "+specName, " (perhaps spec is defined as an object instead of a class?)")
		    	throw e
	    	} }:_*)
	    System.exit(0)	
   }
}

/**
 * JUnit test runner
 */
object JunitRunner{
   def main(args: Array[String]) {
        org.junit.runner.JUnitCore.main(args:_*)
	    System.exit(0)	
   }
}
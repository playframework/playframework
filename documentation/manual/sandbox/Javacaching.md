# Caching

an in-memory cache with expiry support is provided by Play.

_Note: By default, this implementation is used for Play's internal caching as well_

The Java API with information about the usage can be found [here](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/java/play/cache/Cache.java)

# Plugging in your own 
The following simple steps need to be taken if one would like to implement a different caching solution

1. set ```cache.default=disabled``` in ```application.conf```
2. implement ```play.api.CacheAPI``` interface
3. implement ```play.api.Plugin``` and enable your plugin
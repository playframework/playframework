package play.api.libs

object Codec {
    
    def sha1(bytes:Array[Byte]):String = {
        import java.security.MessageDigest
        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        digest.update(bytes)
        digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
    }
    
    def sha1(text:String):String = sha1(text.getBytes)
    
}
package play.libs.crypto;


public interface CSRFTokenSigner {

    public play.api.libs.crypto.CSRFTokenSigner asScala();

    public String extractSignedToken(String token);

    public String generateToken();

    public String generateSignedToken();

    public boolean compareSignedTokens(String tokenA, String tokenB);

    public boolean constantTimeEquals(String a, String b);
}

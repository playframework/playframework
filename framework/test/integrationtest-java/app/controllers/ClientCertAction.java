package controllers;

import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.SimpleResult;

import java.lang.Override;
import java.lang.Throwable;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientCertAction extends Action.Simple {
    @Override
    public F.Promise<SimpleResult> call(final Http.Context context) throws Throwable {
        return context.request().certs(true).flatMap(new F.Function<List<Certificate>, F.Promise<SimpleResult>>() {
            @Override
            public F.Promise<SimpleResult> apply(List<Certificate> certificates) throws Throwable {
                if (certificates.size() > 0 && certificates.get(0) instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certificates.get(0);
                    Matcher matcher = Pattern.compile("CN=([^,]*),").matcher(cert.getSubjectDN().getName());
                    if (matcher.find()) {
                        try {
                            context.request().setUsername(matcher.group(1));
                            return delegate.call(context);
                        } finally {
                            context.request().setUsername(null);
                        }
                    }
                }
                return F.Promise.pure((SimpleResult)unauthorized("No client certificate specified"));
            }
        });
    }
}

/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.filters.csp.*;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

// #csp-report-controller
public class CSPReportController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BodyParser.Of(CSPReportBodyParser.class)
    public Result cspReport(Http.Request request) {
        JavaCSPReport cspReport = request.body().as(JavaCSPReport.class);
        logger.warn("CSP violation: violatedDirective = {}, blockedUri = {}, originalPolicy = {}",
                cspReport.violatedDirective(),
                cspReport.blockedUri(),
                cspReport.originalPolicy());

        return Results.ok();
    }
}
// #csp-report-controller
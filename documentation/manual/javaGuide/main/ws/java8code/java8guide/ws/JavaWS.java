/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.ws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.JsonNode;

import javaguide.testhelpers.MockJavaAction;
import play.mvc.Result;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import org.w3c.dom.Document;
import javax.inject.Inject;

public class JavaWS {
    private static String feedUrl = "http://localhost:3333/feed";
    
    public static class Controller0 extends MockJavaAction {

        private WSClient ws;

        public void responseExamples() {
        
          String url = "http://example.com";
          
          // #ws-response-json
          Promise<JsonNode> jsonPromise = ws.url(url).get().map(response -> {
              return response.asJson();
          });
          // #ws-response-json
          
          // #ws-response-xml
          Promise<Document> documentPromise = ws.url(url).get().map(response -> {
              return response.asXml();
          });
          // #ws-response-xml
          
          // #ws-response-input-stream
          Promise<File> filePromise = ws.url(url).get().map(response -> {
              InputStream inputStream = null;
              OutputStream outputStream = null;
              try {
                  inputStream = response.getBodyAsStream();
              
                  // write the inputStream to a File
                  final File file = new File("/tmp/response.txt");
                  outputStream = new FileOutputStream(file);
              
                  int read = 0;
                  byte[] buffer = new byte[1024];
     
                  while ((read = inputStream.read(buffer)) != -1) {
                      outputStream.write(buffer, 0, read);
                  }
            
                  return file;  
              } catch (IOException e) {
                  throw e;
              } finally {
                  if (inputStream != null) {inputStream.close();}
                  if (outputStream != null) {outputStream.close();}
              } 
          });
          // #ws-response-input-stream
        }
        
        public void patternExamples() {
            String urlOne = "http://localhost:3333/one";
            // #ws-composition
            final Promise<WSResponse> responseThreePromise = ws.url(urlOne).get()
                .flatMap(responseOne -> ws.url(responseOne.getBody()).get())
                .flatMap(responseTwo -> ws.url(responseTwo.getBody()).get());
            // #ws-composition
            
            // #ws-recover
            Promise<WSResponse> responsePromise = ws.url("http://example.com").get();
            Promise<WSResponse> recoverPromise = responsePromise.recoverWith(throwable ->
                ws.url("http://backup.example.com").get()
            );
            // #ws-recover
        }
    }

    public static class Controller1 extends MockJavaAction {

        @Inject WSClient ws;

        // #ws-action
        public Promise<Result> index() {
            return ws.url(feedUrl).get().map(response ->
                ok("Feed title: " + response.asJson().findPath("title").asText())
            );
        }
        // #ws-action
    }

    public static class Controller2 extends MockJavaAction {

        @Inject WSClient ws;

        // #composed-call
        public Promise<Result> index() {
            return ws.url(feedUrl).get()
                     .flatMap(response -> ws.url(response.asJson().findPath("commentsUrl").asText()).get())
                     .map(response -> ok("Number of comments: " + response.asJson().findPath("count").asInt()));
        }
        // #composed-call
    }
    
    public static class OpenIdController extends MockJavaAction {
      
    }

}

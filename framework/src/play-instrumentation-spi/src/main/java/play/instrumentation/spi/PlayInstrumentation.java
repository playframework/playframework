/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.Map;
import java.util.List;

/**
 * The per-request SPI callback interface.
 * <p>
 * For each request to a Play application a new object implementing this interface is created.  Calls to these methods
 * can be made from multiple threads and concurrent calls are possible.
 * <p>
 * Using '<' to mean 'happens before' and '>' for 'happens after'; these callbacks will be invoked with some causal
 * order, as follows:
 * <ul>
 *   <li> recordRequestStart < recordInputHeader
 *   <li> recordInputHeader < recordResolved
 *   <li> recordInputHeader < recordHandlerNotFound
 *   <li> recordResolved < recordRouteRequestResult
 *   <li> recordRouteRequestResult < recordInputProcessingStart
 *   <li> recordInputProcessingStart < recordInputProcessingEnd
 *   <li> recordExpectedInputBodyBytes < recordInputProcessingStart
 *   <li> recordInputProcessingEnd < recordActionStart
 *   <li> recordActionStart < recordOutputProcessingStart
 *   <li> recordActionStart < recordSimpleResult
 *   <li> recordActionStart < recordChunkedResult
 *   <li> recordActionEnd < recordOutputProcessingStart
 *   <li> recordOutputProcessingStart > recordOutputBodyBytes
 *   <li> recordOutputProcessingStart < recordExpectedOutputBodyBytes
 *   <li> recordOutputProcessingStart < recordOutputProcessingEnd
 *   <li> recordExpectedOutputBodyBytes < recordOutputProcessingEnd
 *   <li> recordOutputProcessingEnd < recordRequestEnd
 *   <li> recordOutputBodyBytes < recordOutputProcessingEnd
 * </ul>
 * <p>
 * Further, the following callbacks are guaranteed to be called exactly once:
 * <ul>
 *   <li> recordRequestStart
 *   <li> recordInputHeader
 *   <li> recordOutputHeader
 *   <li> recordResolved
 *   <li> recordRouteRequestResult
 *   <li> recordInputProcessingStart
 *   <li> recordInputProcessingEnd
 *   <li> recordActionStart
 *   <li> recordOutputProcessingStart
 *   <li> recordSimpleResult
 *   <li> recordActionEnd
 *   <li> recordOutputProcessingEnd
 *   <li> recordRequestEnd
 * </ul>
 */
public interface PlayInstrumentation {

  /**
   * Enum for request results
   */
  public static enum RequestResult {
    ESSENTIAL_ACTION("EssentialAction"),
    NO_HANDLER("NoHandler"),
    WEB_SOCKET("WebSocket");

    private String tag = null;

    public String getTag() {
      return tag;
    }

    @Override
    public String toString() {
      return tag;
    }

    RequestResult(String _tag) {
      tag = _tag;
    }
  }

  /**
   * Invoked immediately after the beginning of a request.
   */
  public void recordRequestStart();

  /**
   * Invoked when Play has completed sending a result for a specific request.
   * <p>
   * <b>Note: </b>This is not the same as the <i>actual</i> end of a request from an HTTP point of view.  In particular,
   * a Play action can return a result before all inputs are consumed.
   */
  public void recordRequestEnd();

  /**
   * Marks the beginning of input processing
   */
  public void recordInputProcessingStart();

  /**
   * Marks the end of input processing
   */
  public void recordInputProcessingEnd();

  /**
   * Marks when the beginning of action processing
   */
  public void recordActionStart();

  /**
   * Marks when the result of an action is produced.
   */
  public void recordActionEnd();

  /**
   * Marks the beginning of output processing.
   */
  public void recordOutputProcessingStart();

  /**
   * Marks the end of output processing.
   */
  public void recordOutputProcessingEnd();

  /**
   * Marks the result of routing a request.
   */
  public void recordRouteRequestResult(RequestResult result);

  /**
   * Records data from an error during processing.
   */
  public void recordError(PlayError error);

  /**
   * Records when a handler for a request is not found.
   */
  public void recordHandlerNotFound();

  /**
   * Marks a bad request.
   */
  public void recordBadRequest(String error);

  /**
   * Marks the details of a resolved routing request.
   */
  public void recordResolved(PlayResolved resolved);

  /**
   * Records the status of a simple result
   */
  public void recordSimpleResult(int code);

  /**
   * Records the status of a chunked result.
   */
  public void recordChunkedResult(int code);

  /**
   * Records the metadata of an input chunk
   */
  public void recordInputChunk(PlayHttpChunk chunk);

  /**
   * Records the metadata of an output chunk
   */
  public void recordOutputChunk(PlayHttpChunk chunk);

  /**
   * Records input (request) headers
   */
  public void recordInputHeader(PlayInputHeader inputHeader);

  /**
   * Records the expected number of bytes for input.  Not recorded for chunked inputs.
   */
  public void recordExpectedInputBodyBytes(long bytes);

  /**
   * Records actual body bytes input (received).
   * <p>
   * <b>Note: </b>This method can be called 0 or more times.  The total bytes received should match the expected
   * number of bytes.
   *
   * @see PlayInstrumentation#recordExpectedInputBodyBytes(long)
   */
  public void recordInputBodyBytes(long bytes);

  /**
   * Record output (response) headers.
   */
  public void recordOutputHeader(PlayOutputHeader outputHeader);

  /**
   * Records the expected number of bytes to be output.  Not recorded for chunked outputs.
   */
  public void recordExpectedOutputBodyBytes(long bytes);

  /**
   * Records actual body bytes output (response).
   * <p>
   * <b>Note: </b>This method can be called 0 or more times.  The total bytes output should match the expected
   * number of bytes.
   *
   * @see PlayInstrumentation#recordExpectedOutputBodyBytes(long)
   */
  public void recordOutputBodyBytes(long bytes);
}

package com.segment.analytics.runscope;

public class MessageResponse {
  public static class Message {
    public static class Request {
      public String body;
    }

    public Request request;
  }

  public Message data;
}

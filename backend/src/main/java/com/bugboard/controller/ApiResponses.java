package com.bugboard.controller;

import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Shared API response helpers for consistent response payloads.
 */
public final class ApiResponses {

   private ApiResponses() {
   }

   public static Response error(Response.Status status, String message) {
      return Response.status(status)
            .entity(Map.of("error", message))
            .build();
   }

   public static Response message(Response.Status status, String message) {
      return Response.status(status)
            .entity(Map.of("message", message))
            .build();
   }
}

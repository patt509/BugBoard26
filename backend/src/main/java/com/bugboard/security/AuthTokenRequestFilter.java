package com.bugboard.security;

import java.io.IOException;

import com.bugboard.service.AuthSessionService;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthTokenRequestFilter implements ContainerRequestFilter {

   private static final String USER_ID_HEADER = "X-User-Id";

   @Inject
   private AuthSessionService authSessionService;

   @Override
   public void filter(ContainerRequestContext requestContext) throws IOException {
      String method = requestContext.getMethod();
      String path = requestContext.getUriInfo().getPath();
      if ("OPTIONS".equalsIgnoreCase(method)) {
         return;
      }
      if (isPublicEndpoint(path, method)) {
         return;
      }

      String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
      if (authorization == null || authorization.isBlank()) {
         requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
               .entity(java.util.Map.of("error", "Authorization token is required."))
               .build());
         return;
      }

      try {
         String rawToken = authSessionService.extractBearerToken(authorization);
         AuthTokenClaims claims = authSessionService.validateActiveSession(rawToken);

         requestContext.getHeaders().putSingle(USER_ID_HEADER, String.valueOf(claims.userId()));
         requestContext.setProperty("auth.userId", claims.userId());
         requestContext.setProperty("auth.sessionId", claims.sessionId());
         requestContext.setProperty("auth.role", claims.role());
      } catch (SecurityException ex) {
         requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
               .entity(java.util.Map.of("error", ex.getMessage()))
               .build());
      }
   }

   private boolean isPublicEndpoint(String rawPath, String rawMethod) {
      String method = rawMethod != null ? rawMethod.toUpperCase() : "";
      String path = rawPath != null ? rawPath : "";
      if ("POST".equals(method) && "auth/login".equals(path)) {
         return true;
      }
      if ("POST".equals(method) && "auth/password-reset-request".equals(path)) {
         return true;
      }
      if ("GET".equals(method) && "attachments/info".equals(path)) {
         return true;
      }
      if ("GET".equals(method) && path.startsWith("attachments/issues/")) {
         return true;
      }
      return "GET".equals(method) && path.startsWith("attachments/comments/");
   }
}

package com.bugboard.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling file attachments.
 * Supports only JPG/PNG images with max 5MB size.
 */
@ApplicationScoped
public class AttachmentService {

   private static final Logger logger = Logger.getLogger(AttachmentService.class.getName());

   // Configuration constants
   private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
   private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
         "image/jpeg",
         "image/png");
   private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
         ".jpg",
         ".png");

   // Base directory for storing attachments
   private static final String UPLOAD_DIR = "uploads/attachments";

   /**
    * Extracts the actual file content from a multipart/form-data body.
    * Multipart bodies contain boundary markers and headers that need to be stripped.
    * 
    * @param inputStream The raw multipart input stream
    * @return A clean InputStream containing only the file bytes
    * @throws IOException if reading fails
    */
   public InputStream extractFileFromMultipart(InputStream inputStream) throws IOException {
      // Read entire body into byte array
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] data = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
         buffer.write(data, 0, bytesRead);
      }
      byte[] bodyBytes = buffer.toByteArray();
      
      // Convert to string to find boundaries (for small headers)
      String bodyStr = new String(bodyBytes, 0, Math.min(bodyBytes.length, 2000), "UTF-8");
      
      // Find the start of actual file content (after the headers end with \r\n\r\n)
      int contentStart = -1;
      for (int i = 0; i < bodyBytes.length - 4; i++) {
         if (bodyBytes[i] == '\r' && bodyBytes[i+1] == '\n' && 
             bodyBytes[i+2] == '\r' && bodyBytes[i+3] == '\n') {
            contentStart = i + 4;
            break;
         }
      }
      
      if (contentStart == -1) {
         // No multipart headers found, return as-is
         return new ByteArrayInputStream(bodyBytes);
      }
      
      // Find the boundary at the end (starts with \r\n--)
      int contentEnd = bodyBytes.length;
      for (int i = bodyBytes.length - 1; i >= contentStart + 4; i--) {
         if (bodyBytes[i-3] == '\r' && bodyBytes[i-2] == '\n' && 
             bodyBytes[i-1] == '-' && bodyBytes[i] == '-') {
            // Found end boundary, go back to the \r\n
            contentEnd = i - 3;
            break;
         }
      }
      
      // Extract just the file content
      int length = contentEnd - contentStart;
      if (length <= 0) {
         return new ByteArrayInputStream(bodyBytes);
      }
      
      byte[] fileContent = new byte[length];
      System.arraycopy(bodyBytes, contentStart, fileContent, 0, length);
      
      logger.log(Level.INFO, "Extracted file content: {0} bytes from {1} byte body", 
            new Object[]{length, bodyBytes.length});
      
      return new ByteArrayInputStream(fileContent);
   }

   /**
    * Validates and saves an attachment file.
    * 
    * @param inputStream  The file input stream
    * @param fileName     Original file name
    * @param contentType  MIME type of the file
    * @param fileSize     Size of the file in bytes
    * @param subDirectory Subdirectory (e.g., "issues" or "comments")
    * @param entityId     ID of the entity (issue or comment)
    * @return The relative path where the file was saved
    * @throws IllegalArgumentException if validation fails
    * @throws IOException              if file cannot be saved
    */
   public String saveAttachment(
         InputStream inputStream,
         String fileName,
         String contentType,
         long fileSize,
         String subDirectory,
         Long entityId) throws IOException {

      // Validate file size
      if (fileSize > MAX_FILE_SIZE) {
         throw new IllegalArgumentException(
               String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024)));
      }

      if (fileSize <= 0) {
         throw new IllegalArgumentException("File is empty");
      }

      // Validate content type
      if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
         throw new IllegalArgumentException(
               "Invalid file type. Only JPG and PNG images are allowed.");
      }

      // Validate file extension
      String extension = getFileExtension(fileName);
      if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
         throw new IllegalArgumentException(
               "Invalid file extension. Only .jpg and .png are allowed.");
      }

      // Generate unique filename to prevent overwrites and path traversal
      String uniqueFileName = generateUniqueFileName(entityId, extension);

      // Create directory structure
      Path uploadPath = Paths.get(UPLOAD_DIR, subDirectory, entityId.toString());
      Files.createDirectories(uploadPath);

      // Save file
      Path filePath = uploadPath.resolve(uniqueFileName);
      Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

      // Return relative path for storage in database
      String relativePath = subDirectory + "/" + entityId + "/" + uniqueFileName;

      logger.log(Level.INFO, "Attachment saved: {0}", relativePath);

      return relativePath;
   }

   /**
    * Deletes an attachment file.
    * 
    * @param relativePath The relative path of the attachment
    * @return true if deleted successfully, false otherwise
    */
   public boolean deleteAttachment(String relativePath) {
      if (relativePath == null || relativePath.trim().isEmpty()) {
         return false;
      }

      try {
         Path filePath = Paths.get(UPLOAD_DIR, relativePath);
         boolean deleted = Files.deleteIfExists(filePath);

         if (deleted) {
            logger.log(Level.INFO, "Attachment deleted: {0}", relativePath);
         }

         return deleted;
      } catch (IOException e) {
         logger.log(Level.WARNING, "Failed to delete attachment: " + relativePath, e);
         return false;
      }
   }

   /**
    * Gets the full file path for an attachment.
    * 
    * @param relativePath The relative path stored in database
    * @return The full Path object
    */
   public Path getAttachmentPath(String relativePath) {
      return Paths.get(UPLOAD_DIR, relativePath);
   }

   /**
    * Checks if an attachment file exists.
    */
   public boolean attachmentExists(String relativePath) {
      if (relativePath == null || relativePath.trim().isEmpty()) {
         return false;
      }
      return Files.exists(Paths.get(UPLOAD_DIR, relativePath));
   }

   /**
    * Validates attachment parameters without saving.
    * Useful for pre-validation before processing.
    */
   public void validateAttachment(String contentType, long fileSize, String fileName) {
      if (fileSize > MAX_FILE_SIZE) {
         throw new IllegalArgumentException(
               String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024)));
      }

      if (fileSize <= 0) {
         throw new IllegalArgumentException("File is empty");
      }

      if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
         throw new IllegalArgumentException(
               "Invalid file type. Only JPG and PNG images are allowed.");
      }

      String extension = getFileExtension(fileName);
      if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
         throw new IllegalArgumentException(
               "Invalid file extension. Only .jpg, .jpeg and .png are allowed.");
      }
   }

   // ==================== HELPER METHODS ====================

   private String getFileExtension(String fileName) {
      if (fileName == null || !fileName.contains(".")) {
         return "";
      }
      return fileName.substring(fileName.lastIndexOf("."));
   }

   private String generateUniqueFileName(Long entityId, String extension) {
      return String.format("%d_%s%s",
            entityId,
            UUID.randomUUID().toString().substring(0, 8),
            extension.toLowerCase());
   }

   // Getters for configuration (useful for API documentation)
   public long getMaxFileSize() {
      return MAX_FILE_SIZE;
   }

   public Set<String> getAllowedContentTypes() {
      return ALLOWED_CONTENT_TYPES;
   }

   public Set<String> getAllowedExtensions() {
      return ALLOWED_EXTENSIONS;
   }
}

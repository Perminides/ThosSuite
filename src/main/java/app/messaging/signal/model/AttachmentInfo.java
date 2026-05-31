package app.messaging.signal.model;
public record AttachmentInfo(
    String path,
    String localKey,
    String fileName,
    int size,
    String contentType,
    String attachmentType
) {}
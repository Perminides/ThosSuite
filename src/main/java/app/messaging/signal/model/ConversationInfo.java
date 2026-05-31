package app.messaging.signal.model;
public record ConversationInfo(
    boolean isGroup,
    String name,
    String profileName,
    String profileFamilyName,
    String profileFullName,
    int messageCount,
    String sharedGroupNames
) {}
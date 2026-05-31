package app.messaging.signal.model;
public record ContactInfo(
    String name,
    String profileName,
    String profileFamilyName,
    String profileFullName
) {}
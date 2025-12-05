package app.data;

import java.util.List;

public record SessionProgress (int correct, int incorrect, List<Boolean> details) {}

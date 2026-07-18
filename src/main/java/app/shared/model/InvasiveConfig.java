package app.shared.model;

/** Framework-freie Invasiv-Schwellen für den Tagebuch-Editor (Presenter → DiaryEditor). */
public record InvasiveConfig(int minChars, int minTags, int invasiveSeconds) {}
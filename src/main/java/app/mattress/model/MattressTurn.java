package app.mattress.model;

import java.time.LocalDateTime;

public record MattressTurn(LocalDateTime turnedAt, String direction) {}
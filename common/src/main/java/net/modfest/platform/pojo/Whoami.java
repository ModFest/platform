package net.modfest.platform.pojo;

import org.jspecify.annotations.Nullable;

public record Whoami(boolean isAuthenticated, @Nullable String userId, @Nullable String name) {
}

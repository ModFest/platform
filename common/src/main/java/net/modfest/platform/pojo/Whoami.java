package net.modfest.platform.pojo;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

public record Whoami(boolean isAuthenticated, @Nullable String userId, @Nullable String name, @Nullable Collection<String> permissions) {
}

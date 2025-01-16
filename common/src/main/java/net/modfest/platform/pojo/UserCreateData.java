package net.modfest.platform.pojo;

import lombok.With;

@With
public record UserCreateData(
    String name,
    String pronouns,
    String modrinthId,
    String discordId
) {
}

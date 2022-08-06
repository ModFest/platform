package net.modfest.platform;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import net.modfest.platform.data.DataManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;

public class GuildCommandRegistrar {

    private static final Logger log = Loggers.getLogger(GuildCommandRegistrar.class);

    private final RestClient restClient;
    private final List<ApplicationCommandRequest> commandRequests;
    private final Mono<Long> applicationId;
    private final long guildId;

    private GuildCommandRegistrar(RestClient restClient, List<ApplicationCommandRequest> commandRequests) {
        this.restClient = restClient;
        this.commandRequests = commandRequests;
        this.applicationId = restClient.getApplicationId().cache();
        this.guildId = Long.parseLong(DataManager.getGuildId());
    }

    public static GuildCommandRegistrar create(RestClient restClient, List<ApplicationCommandRequest> commandRequests) {
        return new GuildCommandRegistrar(restClient, commandRequests);
    }

    public Flux<ApplicationCommandData> registerCommands() {
        return bulkOverwriteCommands(commandRequests);
    }

    private Flux<ApplicationCommandData> bulkOverwriteCommands(List<ApplicationCommandRequest> requests) {
        return applicationId.flatMapMany(id -> restClient.getApplicationService()
                .bulkOverwriteGuildApplicationCommand(id, guildId, requests)
                .doOnNext(it -> log.info("Registered command {} to guild {}", it.name(), guildId)));
    }
}
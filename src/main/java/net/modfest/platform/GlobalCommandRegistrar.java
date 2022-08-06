package net.modfest.platform;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;

public class GlobalCommandRegistrar {

    private static final Logger log = Loggers.getLogger(GlobalCommandRegistrar.class);

    private final RestClient restClient;
    private final List<ApplicationCommandRequest> commandRequests;
    private final Mono<Long> applicationId;

    private GlobalCommandRegistrar(RestClient restClient, List<ApplicationCommandRequest> commandRequests) {
        this.restClient = restClient;
        this.commandRequests = commandRequests;
        this.applicationId = restClient.getApplicationId().cache();
    }

    public static GlobalCommandRegistrar create(RestClient restClient, List<ApplicationCommandRequest> commandRequests) {
        return new GlobalCommandRegistrar(restClient, commandRequests);
    }

    public Flux<ApplicationCommandData> registerCommands() {
        return bulkOverwriteCommands(commandRequests);
    }

    private Flux<ApplicationCommandData> bulkOverwriteCommands(List<ApplicationCommandRequest> requests) {
        return applicationId.flatMapMany(id -> restClient.getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(id, requests)
                .doOnNext(it -> log.info("Registered command {} globally" + it, it.name())));
    }
}
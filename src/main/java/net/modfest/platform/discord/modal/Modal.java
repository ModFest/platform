package net.modfest.platform.discord.modal;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import net.modfest.platform.ModFestPlatform;
import org.reactivestreams.Publisher;

import java.util.LinkedList;
import java.util.List;

public abstract class Modal {
    public final String id;
    public final String title;
    public final String command;

    private final List<LayoutComponent> buildComponents = new LinkedList<>();

    public Modal(String id, String title) {
        this.command = id;
        this.id = "modfest-" + id + "-modal";
        this.title = title;
    }

    public String textInput(String id, String label, boolean required, int minLength, int maxLength) {
        id = this.id + "-" + id;
        buildComponents.add(ActionRow.of(TextInput.small(id, label, minLength, maxLength).required(required)));
        return id;
    }

    public Publisher<Void> onCommand(DeferrableInteractionEvent event) {
        var conditions = checkConditions(event);
        if (conditions != null) {
            return conditions;
        }

        return openModal(event);
    }

    public Publisher<Void> checkConditions(DeferrableInteractionEvent event) {
        if (ModFestPlatform.activeEvent == null && this.requiresActiveEvent()) {
            return event.reply("An error has occurred: There is no active ModFest event.");
        }
        return null;
    }

    public Publisher<Void> openModal(DeferrableInteractionEvent event) {
        var builder = InteractionPresentModalSpec.builder().title(this.command).customId(this.id);
        for (LayoutComponent component : this.buildComponents) {
            builder.addComponent(component);
        }
        return event.presentModal(builder.build());
    }

    public abstract Publisher<Void> onSubmit(ModalSubmitInteractionEvent event);

    public boolean requiresActiveEvent() {
        return false;
    }
}

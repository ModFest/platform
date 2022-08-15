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
    private final List<LayoutComponent> buildComponents = new LinkedList<>();

    public Modal(String id, String title) {
        this.id = "modfest-" + id + "-modal";
        this.title = title;
    }

    public String textInput(String id, String label, boolean required, int minLength, int maxLength) {
        return textInput(id, label, required, minLength, maxLength, null);
    }

    public String textInput(String id, String label, boolean required, int minLength, int maxLength, String placeholder) {
        id = this.id + "-" + id;
        var textInput = TextInput.small(id, label, minLength, maxLength);
        if (placeholder != null) {
            textInput = textInput.placeholder(placeholder);
        }
        buildComponents.add(ActionRow.of(textInput.required(required)));
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
            return event.reply("An error has occurred: There is no active ModFest event.").withEphemeral(true);
        }
        return null;
    }

    public Publisher<Void> openModal(DeferrableInteractionEvent event) {
        var builder = InteractionPresentModalSpec.builder().title(this.title).customId(this.id);
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

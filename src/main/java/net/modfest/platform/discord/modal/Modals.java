package net.modfest.platform.discord.modal;

import java.util.HashMap;

public class Modals {

    public static HashMap<String, Modal> MODAL_COMMANDS = new HashMap<>();
    public static HashMap<String, Modal> MODAL_IDS = new HashMap<>();

    static {
        var registerModal = new RegisterModal();
        MODAL_COMMANDS.put(registerModal.command, registerModal);
        MODAL_IDS.put(registerModal.id, registerModal);
    }
}

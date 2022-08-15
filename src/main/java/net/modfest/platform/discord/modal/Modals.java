package net.modfest.platform.discord.modal;

import java.util.HashMap;

public class Modals {

    public static HashMap<String, Modal> MODAL_COMMANDS = new HashMap<>();
    public static HashMap<String, Modal> MODAL_IDS = new HashMap<>();

    public static final Modal REGISTER = add(new RegisterModal(), "register");
    public static final Modal SUBMIT = add(new SubmitModal());

    private static Modal add(Modal modal) {
        return add(modal, null);
    }

    private static Modal add(Modal modal, String command) {
        MODAL_IDS.put(modal.id, modal);
        if (command != null) {
            MODAL_COMMANDS.put(command, modal);
        }
        return modal;
    }
}

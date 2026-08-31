package com.joelj.mcasocial.social;

public enum GiftRating {
    SIGNATURE("favorite"),
    FAVORITE("favorite"),
    LIKED("liked"),
    NEUTRAL("neutral"),
    DISLIKED("disliked");

    private final String dialogueKey;

    GiftRating(String dialogueKey) {
        this.dialogueKey = dialogueKey;
    }

    public String dialogueKey() {
        return dialogueKey;
    }
}

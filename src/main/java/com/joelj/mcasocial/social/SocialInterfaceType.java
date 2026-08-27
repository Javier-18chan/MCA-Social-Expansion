package com.joelj.mcasocial.social;

public final class SocialInterfaceType {
    public static final int PHONE = 0;
    public static final int LETTER = 1;
    public static final int CRYSTAL = 2;

    private SocialInterfaceType() {}

    public static int normalize(int interfaceType) {
        return interfaceType >= PHONE && interfaceType <= CRYSTAL ? interfaceType : PHONE;
    }
}

package me.onethecrazy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class ToastUtil {
    public static void showFileTooLargeToast(){
        // Get the singleton client instance
        Minecraft client = Minecraft.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        SystemToast.add(
                client.gui.toastManager(),
                SystemToast.SystemToastId.FILE_DROP_FAILURE,
                Component.translatable("gui.fbxplayermodels.title.file_too_large"),
                Component.translatable("gui.fbxplayermodels.description.file_too_large")
        );
    }

    public static void showModerationNoticeToast(){
        // Get the singleton client instance
        Minecraft client = Minecraft.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        SystemToast.addOrUpdate(
                client.gui.toastManager(),
                SystemToast.SystemToastId.UNSECURE_SERVER_WARNING,
                Component.translatable("gui.fbxplayermodels.title.moderation_notice"),
                Component.translatable("gui.fbxplayermodels.description.moderation_notice")
        );
    }
}

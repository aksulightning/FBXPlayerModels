package me.onethecrazy.util;

import me.onethecrazy.FBXPlayerModelsMod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NativeFileDialog;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientFileUtil {
    private static boolean isNfdInitialized = false;
    private static @Nullable CompletableFuture<@Nullable String> currentOpenFileDialog = new CompletableFuture<String>();
    private static final ExecutorService DIALOG_THREAD =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "FBXPlayerModelsMod-Native-Dialog-Thread"));

    public static CompletableFuture<String> modelPickerDialog() {
        FBXPlayerModelsMod.LOGGER.info("Opening file picker...");
        currentOpenFileDialog = new CompletableFuture<>();

        Runnable dialogTask = () -> {
            if (!isNfdInitialized) {
                NativeFileDialog.NFD_Init();
                isNfdInitialized = true;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer out = stack.callocPointer(1);

                NFDFilterItem.Buffer filtersBuffer = NFDFilterItem.malloc(1);
                filtersBuffer.get(0)
                        .name(stack.UTF8("FBX Model"))
                        .spec(stack.UTF8("fbx"));

                int result = NativeFileDialog.NFD_OpenDialog(out, filtersBuffer, "");

                if (result == NativeFileDialog.NFD_OKAY) {
                    currentOpenFileDialog.complete(out.getStringUTF8(0));
                } else {
                    currentOpenFileDialog.complete(null);
                }
            } catch (Exception e) {
                FBXPlayerModelsMod.LOGGER.error("Ran into error while opening File Dialog: ", e);
            }
        };

        DIALOG_THREAD.submit(dialogTask);

        return currentOpenFileDialog;
    }
}

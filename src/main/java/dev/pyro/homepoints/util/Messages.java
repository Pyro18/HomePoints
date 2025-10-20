package dev.pyro.homepoints.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Messages {

    public static Text success(String message) {
        return Text.literal(message).formatted(Formatting.GREEN);
    }

    public static Text error(String message) {
        return Text.literal(message).formatted(Formatting.RED);
    }

    public static Text info(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    public static Text highlight(String message) {
        return Text.literal(message).formatted(Formatting.AQUA);
    }

    public static Text shareHomeMessage(String homeName, String fromPlayer) {
        return Text.literal("")
                .append(Text.literal(fromPlayer).formatted(Formatting.GOLD))
                .append(Text.literal(" wants to share home ").formatted(Formatting.YELLOW))
                .append(Text.literal(homeName).formatted(Formatting.AQUA))
                .append(Text.literal(" with you! ").formatted(Formatting.YELLOW))
                .append(Text.literal("[Accept]")
                        .styled(style -> style
                                .withColor(Formatting.GREEN)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/acceptshare " + fromPlayer + " " + homeName
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Click to accept and add to your homes")
                                ))
                        ));
    }
}

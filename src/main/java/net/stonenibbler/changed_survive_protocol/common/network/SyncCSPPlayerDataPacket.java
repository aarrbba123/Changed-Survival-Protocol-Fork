package net.stonenibbler.changed_survive_protocol.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.stonenibbler.changed_survive_protocol.client.CSPClientData;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;

import java.util.function.Supplier;

public record SyncCSPPlayerDataPacket(double infectionPercent,
                                      double coverage,
                                      boolean infected,
                                      String strainId,
                                      double lucidity,
                                      boolean lucidityActive) {
    public static SyncCSPPlayerDataPacket from(CSPPlayerData data) {
        return new SyncCSPPlayerDataPacket(
                data.getInfectionPercent(),
                data.getCoverage(),
                data.isInfected(),
                data.getActiveStrainId(),
                data.getLucidity(),
                data.isLucidityActive());
    }

    public static void encode(SyncCSPPlayerDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.infectionPercent);
        buffer.writeDouble(packet.coverage);
        buffer.writeBoolean(packet.infected);
        buffer.writeUtf(packet.strainId);
        buffer.writeDouble(packet.lucidity);
        buffer.writeBoolean(packet.lucidityActive);
    }

    public static SyncCSPPlayerDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncCSPPlayerDataPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readDouble(),
                buffer.readBoolean());
    }

    public static void handle(SyncCSPPlayerDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CSPClientData.set(packet));
        context.setPacketHandled(true);
    }
}

package com.smartbear.mqttsupport;

public interface CancellationToken {
    boolean cancelled();

    String cancellationReason();
}

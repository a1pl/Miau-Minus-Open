package miau.event.callables;

import miau.event.Cancellable;
import miau.event.Event;

public abstract class EventCancellable implements Event, Cancellable {
    private boolean cancelled;

    protected EventCancellable() {
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        this.cancelled = state;
    }
}

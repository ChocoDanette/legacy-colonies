package com.chocodanette.legacycolonies.colony;

/**
 * Who builds a hut, chosen once: the first work order given to the builder, or the first building confirmed by hand.
 */
public enum BuildMode
{
    /**
     * Every level comes from the builder and the plan; the hut window no longer shows "confirm building".
     */
    BUILDER,
    /**
     * Every level is confirmed by hand against the rules; the builder no longer takes work orders for the hut.
     */
    HAND
}

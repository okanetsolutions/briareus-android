package com.okanetsolutions.briareus.core

object SessionActions {
    fun canCompact(session: Session): Boolean = session.raw.bool("canCompact") == true && session.raw.bool("compacting") != true
    fun canClear(session: Session): Boolean = session.raw.str("kind") == "devchat" && !session.isActive && session.raw.bool("compacting") != true
}

package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.LogEntry;

public class OnLogEntryChanged {

  public LogEntry entry;

  public OnLogEntryChanged(LogEntry entry) {
      this.entry = entry;
  }
}

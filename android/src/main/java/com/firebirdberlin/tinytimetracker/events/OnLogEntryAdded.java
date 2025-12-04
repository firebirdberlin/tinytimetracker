package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.LogEntry;

public class OnLogEntryAdded {

  public LogEntry entry;

  public OnLogEntryAdded(LogEntry entry) {
      this.entry = entry;
  }
}

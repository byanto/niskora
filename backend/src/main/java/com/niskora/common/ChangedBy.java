package com.niskora.common;

import java.util.UUID;

/** The user who made a change, as a refused change names them (FE-81): their id and full name. */
public record ChangedBy(UUID id, String fullName) {}

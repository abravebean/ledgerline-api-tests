package com.ledgerline.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One bank account as the API returns it. Money is always in integer cents. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(String id, String type, String name, String number, long balanceCents, String balance) {
}

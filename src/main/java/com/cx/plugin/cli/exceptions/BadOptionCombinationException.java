package com.cx.plugin.cli.exceptions;

public class BadOptionCombinationException extends CLIParsingException {
    public BadOptionCombinationException(String message) {
        super(message);
    }
}

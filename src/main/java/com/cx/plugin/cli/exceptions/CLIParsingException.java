package com.cx.plugin.cli.exceptions;

/**
 * Created by idanA on 11/5/2018.
 */
public class CLIParsingException extends Exception {

    public CLIParsingException(String message) {
        super(message);
    }

    public CLIParsingException(String message, Exception e) {
        super(message, e);
    }
}

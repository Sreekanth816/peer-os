package io.subutai.common.peer;


public class Timeouts
{
    public static final int COMMAND_REQUEST_MESSAGE_TIMEOUT = 30;
    public static final int CREATE_CONTAINER_REQUEST_TIMEOUT = 3 * 60;
    public static final int CREATE_CONTAINER_RESPONSE_TIMEOUT = 3 * 60 * 60; // 3 hours for containers clone
    public static final int PREPARE_TEMPLATES_REQUEST_TIMEOUT = 3 * 60;
    public static final int PREPARE_TEMPLATES_RESPONSE_TIMEOUT = 24 * 60 * 60; // 24 hours for template import
    public static final int PEER_MESSAGE_TIMEOUT = 30;
}

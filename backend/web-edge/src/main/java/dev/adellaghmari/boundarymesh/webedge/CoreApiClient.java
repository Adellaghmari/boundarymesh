package dev.adellaghmari.boundarymesh.webedge;

interface CoreApiClient {
    CoreResponse call(String method, String path, byte[] body);

    record CoreResponse(int status, String body) {}
}

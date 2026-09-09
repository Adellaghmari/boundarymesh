package dev.adellaghmari.boundarymesh.domain.identity;

public enum BoundaryPermission {
    BOUNDARY_READ("boundary.read"),
    BOUNDARY_EVALUATE("boundary.evaluate"),
    BOUNDARY_ADMIN("boundary.admin");

    private final String scope;

    BoundaryPermission(String scope) {
        this.scope = scope;
    }

    public String scope() {
        return scope;
    }
}

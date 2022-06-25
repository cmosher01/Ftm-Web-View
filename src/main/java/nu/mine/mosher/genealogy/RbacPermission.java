package nu.mine.mosher.genealogy;

//import java.util.*;

public enum RbacPermission {
//    PUBLIC(Set.of()),
//    PRIVATE(Set.of(PUBLIC)),
//    LIST(Set.of(PUBLIC)),
//    READ(Set.of(LIST)),
//    ;
//
//    private final Set<RbacPermission> implies;
//
//    RbacPermission(final Set<RbacPermission> implies) {
//        this.implies = implies;
//    }
//
//    public void addImpliedTo(final Collection<RbacPermission> s) {
//        s.add(this);
//        // this assumes graph is acyclic:
//        this.implies.forEach(i -> i.addImpliedTo(s));
//    }
    PUBLIC, PRIVATE, LIST, READ
}

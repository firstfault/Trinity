package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.execution.MemberDetails;

import java.util.Objects;

record IdentityMemberKey(String owner, String name, String descriptor) {
    IdentityMemberKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    static IdentityMemberKey of(MemberDetails details) {
        return new IdentityMemberKey(details.getOwner(), details.getName(), details.getDesc());
    }

    MemberDetails details() {
        return new MemberDetails(owner, name, descriptor);
    }

    String display() {
        return owner + '.' + name + descriptor;
    }
}

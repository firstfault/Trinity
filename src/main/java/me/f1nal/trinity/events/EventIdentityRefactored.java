package me.f1nal.trinity.events;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.refactor.identity.IdentityRefactorRequest;

import java.util.Map;
import java.util.Set;

/** A single committed project-wide JVM identity transaction. */
public record EventIdentityRefactored(
        IdentityRefactorRequest request,
        Map<String, String> classMappings,
        Map<MemberDetails, MemberDetails> memberMappings,
        Set<ClassInput> affectedClasses) {

    public EventIdentityRefactored {
        classMappings = Map.copyOf(classMappings);
        memberMappings = Map.copyOf(memberMappings);
        affectedClasses = Set.copyOf(affectedClasses);
    }
}

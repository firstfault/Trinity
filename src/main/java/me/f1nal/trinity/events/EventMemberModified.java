package me.f1nal.trinity.events;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MemberInput;

public final class EventMemberModified {
    private final MemberInput<?> memberInput;
    private final MemberDetails previousDetails;

    public EventMemberModified(MemberInput<?> memberInput) {
        this(memberInput, memberInput.getDetails());
    }

    public EventMemberModified(MemberInput<?> memberInput, MemberDetails previousDetails) {
        this.memberInput = memberInput;
        this.previousDetails = previousDetails;
    }

    public MemberInput<?> getMemberInput() {
        return memberInput;
    }

    public MemberDetails getPreviousDetails() {
        return previousDetails;
    }

    public ClassInput getClassInput() {
        return memberInput.getOwningClass();
    }
}

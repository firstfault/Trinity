package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;
import me.f1nal.trinity.decompiler.struct.match.IMatchable;
import me.f1nal.trinity.decompiler.struct.match.MatchEngine;
import me.f1nal.trinity.decompiler.struct.match.MatchNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchEngineConcurrencyTest {
    @Test
    void capturesAreIsolatedBetweenMethodWorkers() throws Exception {
        int workers = 8;
        MatchEngine engine = new MatchEngine("exprent type:var index:$index$");
        CyclicBarrier matched = new CyclicBarrier(workers);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<Integer>> captures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                int expected = index;
                captures.add(executor.submit(() -> {
                    assertTrue(engine.match(new CapturingVariable(expected)));
                    matched.await();
                    return (Integer) engine.getVariableValue("$index$");
                }));
            }
            for (int index = 0; index < workers; index++) {
                assertEquals(index, captures.get(index).get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private record CapturingVariable(int index) implements IMatchable {
        @Override
        public IMatchable findObject(MatchNode matchNode, int childIndex) {
            return null;
        }

        @Override
        public boolean match(MatchNode matchNode, MatchEngine engine) {
            for (Map.Entry<MatchProperties, MatchNode.RuleValue> rule
                    : matchNode.getRules().entrySet()) {
                if (rule.getKey() == MatchProperties.EXPRENT_TYPE
                        && !Integer.valueOf(Exprent.EXPRENT_VAR).equals(rule.getValue().value)) {
                    return false;
                }
                if (rule.getKey() == MatchProperties.EXPRENT_VAR_INDEX
                        && !engine.checkAndSetVariableValue(
                        rule.getValue().value.toString(), index)) {
                    return false;
                }
            }
            return true;
        }
    }
}

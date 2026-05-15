package io.deephaven.ui

import io.deephaven.engine.liveness.LivenessScope
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.render.RenderContext
import spock.lang.Specification

/**
 * Tests for the LivenessScope plumbing on RenderContext and the hooks that drive it
 * ({@code useMemo} and {@code useLivenessScope}). Verifies the same cross-render lifecycle that
 * the Python plugin relies on: scopes managed this render survive until they aren't re-managed,
 * scopes from the previous render are released after a successful render, and an in-flight render
 * failure keeps the old scopes around so live objects aren't released mid-error.
 */
class LivenessSpec extends Specification {

    /**
     * Probe whether a LivenessScope has been released. {@code tryRetainReference()} returns
     * {@code false} once refcount has hit zero; otherwise we successfully retained (count++) and
     * must drop again to restore the original count.
     */
    private static boolean isReleased(LivenessScope scope) {
        if (scope.tryRetainReference()) {
            scope.dropReference()
            return false
        }
        return true
    }

    def "each render gets a fresh top-level scope, and the prior one is released on the next render"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        LivenessScope first
        LivenessScope second

        when: "first render"
        ctx.open().withCloseable {
            first = ctx.topLevelScope()
        }

        then: "scope exists and is still alive immediately after close (collected_scopes retains it)"
        first != null
        !isReleased(first)

        when: "second render — the prior top-level falls out of collected_scopes and is released"
        ctx.open().withCloseable {
            second = ctx.topLevelScope()
        }

        then:
        second != null
        !second.is(first)
        isReleased(first)
    }

    def "manage retains a scope across one render but releases it the next time it isn't managed"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def external = new LivenessScope()

        when: "first render manages the external scope"
        ctx.open().withCloseable {
            ctx.manage(external)
        }

        then: "the scope survives the close because we owned it via manage()"
        !isReleased(external)

        when: "second render doesn't manage it"
        ctx.open().withCloseable { /* no manage */ }

        then: "stale scope is released as part of close()"
        isReleased(external)
    }

    def "manage keeps a scope alive across many renders so long as it's re-managed each time"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def scope = new LivenessScope()

        when:
        3.times {
            ctx.open().withCloseable {
                ctx.manage(scope)
            }
        }

        then:
        !isReleased(scope)
    }

    def "unmount releases all collected scopes"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def a = new LivenessScope()
        def b = new LivenessScope()

        ctx.open().withCloseable {
            ctx.manage(a)
            ctx.manage(b)
        }

        when:
        ctx.unmount()

        then:
        isReleased(a)
        isReleased(b)
    }

    def "a failed render keeps old scopes around for the next successful render to reconcile"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def kept = new LivenessScope()

        ctx.open().withCloseable {
            ctx.manage(kept)
        }

        when: "next render fails before re-managing — caller marks failure"
        def scope = ctx.open()
        try {
            scope.markBodyFailed()
        } finally {
            scope.close()
        }

        then: "kept scope is NOT released; it merges back into collected_scopes"
        !isReleased(kept)

        when: "a successful render that still doesn't re-manage finally drops it"
        ctx.open().withCloseable { /* no manage */ }

        then:
        isReleased(kept)
    }

    def "useMemo creates a scope on first run and re-manages it across cached renders"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        int invocations = 0
        def deps = [1]
        LivenessScope memoScope

        when: "first render — compute and capture the scope from collectedScopes"
        Integer firstValue
        ctx.open().withCloseable {
            firstValue = Hooks.useMemo({ -> invocations++; 7 } as java.util.function.Supplier, deps)
            // collectedScopes is {topLevel, memo-scope}; the memo scope is the one that's not topLevel
            memoScope = ctx.collectedScopes().find { !it.is(ctx.topLevelScope()) }
        }

        then:
        firstValue == 7
        memoScope != null
        !isReleased(memoScope)

        when: "render again with the same deps — supplier doesn't re-run but the scope re-manages"
        Integer secondValue
        ctx.open().withCloseable {
            secondValue = Hooks.useMemo({ -> invocations++; 7 } as java.util.function.Supplier, deps)
        }

        then:
        secondValue == 7
        invocations == 1
        !isReleased(memoScope)

        when: "render with new deps — fresh scope created, old released"
        ctx.open().withCloseable {
            Hooks.useMemo({ -> invocations++; 99 } as java.util.function.Supplier, [2])
        }

        then:
        invocations == 2
        isReleased(memoScope)
    }

    def "useLivenessScope captures a scope on wrapper invocation and transfers it on next render"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        int invocations = 0
        Closure wrapped

        when: "first render with deps=[1] — wrapper created, no scope captured yet"
        ctx.open().withCloseable {
            wrapped = Hooks.useLivenessScope({ -> invocations++ }, [1])
        }
        // Invoke wrapped outside the render — this is the use case the hook exists for
        wrapped.call()

        then: "invocation succeeded; a scope was lazily created by the wrapper"
        invocations == 1

        when: "render with deps=[2] — make_wrapper re-runs, transfers the captured scope"
        int collectedCount = -1
        LivenessScope managed
        ctx.open().withCloseable {
            Hooks.useLivenessScope({ -> invocations++ }, [2])
            collectedCount = ctx.collectedScopes().size()
            // The managed (non-topLevel, non-memo) scope is the one transferred from the wrapper.
            managed = ctx.collectedScopes().find { !it.is(ctx.topLevelScope()) && !isReleased(it) }
        }

        then: "scope is now owned by RenderContext and not released"
        // collectedScopes contains: topLevel + memo-scope (re-created on dep change) + transferred scope
        collectedCount == 3
        managed != null
        !isReleased(managed)
    }

    def "useLivenessScope with a Runnable wraps and lets state survive callback invocation"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        int hits = 0
        Runnable wrapped

        when:
        ctx.open().withCloseable {
            wrapped = Hooks.useLivenessScope({ hits++ } as Runnable, [])
        }
        wrapped.run()
        wrapped.run()

        then:
        hits == 2
    }
}

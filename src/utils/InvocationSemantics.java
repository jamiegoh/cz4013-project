package utils;
// invocation semantics
// at-least-once (server processes request multiple times, does not work well for non-idempotent requests)
// at-most-once (server processes request once, by storing request id and request output)
public enum InvocationSemantics {
    AT_LEAST_ONCE,
    AT_MOST_ONCE
}

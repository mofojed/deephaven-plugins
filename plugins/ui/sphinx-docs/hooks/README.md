# Hooks

Special functions prefixed with `use_` are called _hooks_. These functions must only be used at the _top_ of a `@ui.component` or another hook. If you want to use one in a conditional or a loop, extract that logic to a new component and put it there.

## General hooks

General hooks for providing state, memoization, and other essential functionality for creating custom components.

```{toctree}
:glob:
:maxdepth: 1
general/*
```

## Table hooks

Use these hooks when you want to use data from a table on that python side:

```{toctree}
:glob:
:maxdepth: 1
table/*
```

## Advanced hooks

These hooks should only be used if you know what you're doing.

```{toctree}
:glob:
:maxdepth: 1
advanced/*
```

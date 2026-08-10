# Bug report — ready to paste into https://bugreport.java.com

The headings below match the fields of the submission form one to one. The form has no
product/category selector, so the Description names the affected component in its first line.
Paste `ScrollPaneSnapTest.java` into the "Source code" field.

---

## Synopsis

JavaFX 26 regression: ScrollPaneSkin compute methods round their result down and return a
preferred size smaller than the content requires (caused by JDK-8370652)

## Classification (form fields)

- **Operating System:** Windows 11 Pro, build 26200, x64
- **Java version:** 25.0.2 (the JDK version is not the trigger — see Description)
- **Did this work on an earlier release:** Yes
- **Last working Java version:** 25 (with JavaFX 25; the same JDK with JavaFX 26 fails)
- **Frequency:** always

## Description

This is an OpenJFX issue, component `javafx`, subcomponent `controls`, in
`javafx.scene.control.skin.ScrollPaneSkin`. It is a regression between **JavaFX 25**
(`javafx.runtime.version = 25+29`, works) and **JavaFX 26** (`26+27`, broken). The JDK version is
irrelevant: reproduced on JDK 25.0.2 and on JDK 26. Tested on Windows 11, but the affected code is
platform independent control code.

JDK-8370652 ("Control and ScrollPaneSkin should snap computed width/height values to prevent
scrollbars appearing due to rounding errors", fixed in JavaFX 26) wrapped the results of the four
compute methods of `ScrollPaneSkin` in `snapSpaceX`/`snapSpaceY`:

```java
// JavaFX 25
else if (sp.getContent() != null) {
    return (sp.getContent().prefWidth(height) + minWidth);
}

// JavaFX 26
else if (sp.getContent() != null) {
    return snapSpaceX(sp.getContent().prefWidth(height) + minWidth);
}
```

`snapSpace` **rounds** (`Math.round`), whereas `snapSize` **ceils** (`Math.ceil`). Rounding is
correct for spacing and for differences, but not for a preferred size: whenever the fractional part
of the computed size is below 0.5, the ScrollPane now reports a preferred width that is *smaller
than the preferred width its own content asked for*. The content is then laid out narrower than it
requested.

For content whose height depends on its width — i.e. any wrapped text — the consequence is not
cosmetic. Losing a fraction of a pixel adds a whole line, the content no longer fits the viewport,
and an `AS_NEEDED` scroll bar appears. The scroll bar takes further width, which can add yet
another line. This is the exact symptom JDK-8370652 set out to prevent.

Before JDK-8370652 the raw, unsnapped value was passed up to the parent, which applied its own
`snapSize` (ceil) — for example `DialogPane.computePrefWidth` ends in `snapSizeX(...)`. The pane
therefore always ended up at least as wide as its content required.

Note that ceiling here can never reintroduce the problem that JDK-8370652 fixed: a preferred size
that is up to one pixel too large cannot cause a spurious scroll bar, while a preferred size that
is a fraction of a pixel too small demonstrably can. The concern voiced in the review (ceil would
amplify errors that are one ulp above the desired value) applies to the subtractions in
`Control.layoutChildren`, not to the preferred sizes returned by these compute methods.

## System information

(output of `java -XshowSettings:properties -version`)

## Steps to Reproduce

Run the attached single-file source program, first against JavaFX 25, then against JavaFX 26:

```
java --module-path <javafx-25-lib> --add-modules javafx.controls ScrollPaneSnapTest.java
java --module-path <javafx-26-lib> --add-modules javafx.controls ScrollPaneSnapTest.java
```

It puts a `Region` into a `ScrollPane` (`fitToWidth = true`, vbar policy `AS_NEEDED`) inside an
auto-sized window. The Region stands in for a wrapped `Label`: it reports a natural width with a
fractional part (300.2) and needs a second "line" as soon as it is given less. No fonts are
involved, so the result does not depend on platform text metrics. The program forces a render scale
of 1 so the numbers are readable; the regression is not caused by display scaling.

## Expected Result

JavaFX 25:

```
PART A - preferred width must cover the content
  content natural width  = 300.2
  ScrollPane insets      = 20.0
  needed (content+insets)= 320.2
  ScrollPane.prefWidth   = 320.2
  ok: preferred width covers the content

PART B - visible consequence
  ScrollPane width       = 320.2
  viewport width         = 301.0
  viewport height        = 100.0
  content height         = 100.0
  vertical scroll bar    = not shown
  PASS
```

## Actual Result

JavaFX 26:

```
PART A - preferred width must cover the content
  content natural width  = 300.2
  ScrollPane insets      = 20.0
  needed (content+insets)= 320.2
  ScrollPane.prefWidth   = 320.0
  FAIL: preferred width is 0.2 px smaller than the content needs

PART B - visible consequence
  ScrollPane width       = 320.0
  viewport width         = 287.0
  viewport height        = 100.0
  content height         = 200.0
  vertical scroll bar    = SHOWN
  FAIL (unwanted scroll bar)
```

## Real-world impact

Found in a JavaFX application whose message dialogs put the message into a
`ScrollPane(Label with wrapText)` and let the dialog size itself. Measured with the same code and
the same text under both versions:

| | JavaFX 25 | JavaFX 26 |
|---|---|---|
| natural width of the longest text line | 1356.152 | 1356.152 |
| DialogPane width | 1391 | 1390 |
| viewport width | 1357 | 1334 |
| label height | 120 (5 lines) | 144 (6 lines) |
| vertical scroll bar | not shown | shown |

Widening the dialog by exactly one pixel under JavaFX 26 restores the JavaFX 25 layout in every
measured value. Whether an individual dialog is affected depends on the fractional part of its text
width, i.e. it looks random: of the font sizes 16/18/20/22/24 for the same message, 20 and 24 show
the scroll bar and 16/18/22 do not — precisely those whose fractional part is below 0.5.

The dialog is a plain `Dialog`/`Alert`; `StageStyle.EXTENDED` and `HeaderBar` are not involved
(verified with a decorated dialog without a `HeaderBar`).

## Suggested Fix

In `ScrollPaneSkin.computePrefWidth`, `computePrefHeight`, `computeMinWidth` and `computeMinHeight`,
use `snapSizeX`/`snapSizeY` (ceil) instead of `snapSpaceX`/`snapSpaceY` (round). A size returned by
these methods must never be smaller than the size the content asked for.

If amplification of one-ulp errors is a concern, ceil with a small tolerance
(`Math.ceil(value - epsilon)`) would satisfy both goals. The change in `Control.layoutChildren`
introduced by JDK-8370652 computes a difference and can keep `snapSpace`.

## Workaround

Make the preferred width of the *content* a whole number, so that nothing is left for the skin to
round away. For a wrapped label, after CSS has been applied:

```java
label.setPrefWidth(Math.ceil(label.prefWidth(-1)));
```

Verified: this produces byte-identical layout values under JavaFX 25 and 26.

Note that overriding `ScrollPane.computePrefWidth` and ceiling `super.computePrefWidth(height)`
does *not* help — the value has already been rounded down inside the skin, and ceiling a whole
number is a no-op. Adding a pixel (`super.computePrefWidth(height) + 1`) does work, but widens
every such pane by one pixel on every version.

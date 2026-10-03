/*
 * Regenerates the two golden fixtures of the plan-274 S4 design token deriver from BOOTSTRAP'S OWN SASS:
 *
 *   bootstrap-function-golden.json   (BootstrapColorFunctionsTest, BootstrapColorDeriverTest)
 *       per input colour: tint/shade/shift/contrast/luminance and every variable Bootstrap derives from a theme colour
 *       (text-emphasis, bg-subtle, border-subtle, link colours, focus helpers), light and dark, plus the output of the
 *       real `button-variant`, `button-outline-variant` and `table-variant` mixins; plus the luminance of every grey 0..255.
 *       Compiled from Bootstrap's scss with Bootstrap's default settings, which is what the react4j-modern theme uses
 *       for everything the deriver mirrors.
 *
 *   theme-sheet-golden.json          (ThemeTokenCssSheetGoldenTest)
 *       per token set: the declarations of the FULL react4j-modern theme, compiled with that token set applied as Sass
 *       variable overrides, that differ from the default compile. This is the oracle for "the renderer emits exactly what
 *       Sass would have compiled".
 *
 * The expected values are never computed by Java and never typed by hand. To regenerate (needs the sass and postcss
 * packages of react4j-core-ui, i.e. an `npm install` there):
 *
 *   node regenerate-golden.js
 *
 * Optional: REACT4J_CORE_UI_REACT_DIR points at react4j-core-ui/src/main/react when this file is run from elsewhere.
 * Regenerate after ANY change of react4j-modern.scss or of the Bootstrap version, then run the react4j-core tests: a
 * failing golden test after a regeneration is exactly the drift this fixture exists to catch.
 */
const fs = require("fs");
const path = require("path");

const reactDir = process.env.REACT4J_CORE_UI_REACT_DIR || path.resolve(__dirname, "../../../../../../react4j-core-ui/src/main/react");
const sass = require(path.join(reactDir, "node_modules", "sass"));
const postcss = require(path.join(reactDir, "node_modules", "postcss"));
const bootstrapScss = path.join(reactDir, "node_modules", "bootstrap", "scss");
const themeScss = fs.readFileSync(path.join(reactDir, "src", "theme", "react4j-modern.scss"), "utf8");
const sassVersion = require(path.join(reactDir, "node_modules", "sass", "package.json")).version;
const bootstrapVersion = require(path.join(reactDir, "node_modules", "bootstrap", "package.json")).version;

// ---------------------------------------------------------------------------------------------------------------------
// Level 1: Bootstrap's functions, variables and mixins
// ---------------------------------------------------------------------------------------------------------------------

const FUNCTION_COLORS = ["#4f46e5", "#000000", "#ffffff", "#ffc107", "#ffe066", "#0f766e", "#808080", "#d63384", "#0d6efd", "#198754", "#6c757d", "#0dcaf0",
    "#dc3545", "#0a0a0a", "#fafafa", "#777777", "#7c3aed", "#f59e0b", "#0e9f6e", "#be123c", "#336699"];

function declarations(css, selector) {
    const result = {};
    postcss.parse(css).walkRules((rule) => {
        if (rule.selector === selector) {
            rule.walkDecls((d) => {
                result[d.prop] = d.value.replace(/\s+/g, " ");
            });
        }
    });
    return result;
}

function functionCase(hex) {
    const source = `
@import "functions";
$primary: ${hex};
@import "variables";
@import "variables-dark";
@import "maps";
@import "mixins";
.values {
  --tint-40: #{tint-color($primary, 40%)};
  --tint-50: #{tint-color($primary, 50%)};
  --tint-60: #{tint-color($primary, 60%)};
  --tint-70: #{tint-color($primary, 70%)};
  --tint-80: #{tint-color($primary, 80%)};
  --shade-15: #{shade-color($primary, 15%)};
  --shade-20: #{shade-color($primary, 20%)};
  --shade-25: #{shade-color($primary, 25%)};
  --shade-40: #{shade-color($primary, 40%)};
  --shade-60: #{shade-color($primary, 60%)};
  --shade-80: #{shade-color($primary, 80%)};
  --shift-20: #{shift-color($primary, 20%)};
  --shift-minus-80: #{shift-color($primary, -80%)};
  --mix-white-7_5: #{mix(white, $primary, 7.5%)};
  --luminance: #{luminance($primary)};
  --contrast-ratio-white: #{contrast-ratio($primary, white)};
  --contrast-ratio-black: #{contrast-ratio($primary, black)};
  --color-contrast: #{color-contrast($primary)};
  --to-rgb: #{to-rgb($primary)};
  --text-emphasis: #{$primary-text-emphasis};
  --bg-subtle: #{$primary-bg-subtle};
  --border-subtle: #{$primary-border-subtle};
  --text-emphasis-dark: #{$primary-text-emphasis-dark};
  --bg-subtle-dark: #{$primary-bg-subtle-dark};
  --border-subtle-dark: #{$primary-border-subtle-dark};
  --link-color: #{$link-color};
  --link-hover-color: #{$link-hover-color};
  --link-color-dark: #{$link-color-dark};
  --link-hover-color-dark: #{$link-hover-color-dark};
  --input-focus-border-color: #{$input-focus-border-color};
  --range-thumb-active-bg: #{$form-range-thumb-active-bg};
  --btn-link-focus-shadow-rgb: #{$btn-link-focus-shadow-rgb};
  --link-helper-hover: #{if(color-contrast($primary) == $color-contrast-light, shade-color($primary, $link-shade-percentage), tint-color($primary, $link-shade-percentage))};
}
.button-variant { @include button-variant($primary, $primary); }
.button-outline-variant { @include button-outline-variant($primary); }
@include table-variant("x", shift-color($primary, $table-bg-scale));
`;
    const css = sass.compileString(source, { loadPaths: [bootstrapScss], style: "expanded", quietDeps: true, logger: sass.Logger.silent, silenceDeprecations: ["import"] }).css;
    return {
        input: hex,
        values: declarations(css, ".values"),
        buttonVariant: declarations(css, ".button-variant"),
        outlineVariant: declarations(css, ".button-outline-variant"),
        tableVariant: declarations(css, ".table-x"),
    };
}

function greyLuminance() {
    const source = `
@import "functions";
@import "variables";
@for $i from 0 through 255 {
  .g#{$i} { --l: #{luminance(rgb($i, $i, $i))}; }
}
`;
    const css = sass.compileString(source, { loadPaths: [bootstrapScss], style: "expanded", quietDeps: true, logger: sass.Logger.silent, silenceDeprecations: ["import"] }).css;
    const result = [];
    for (let i = 0; i < 256; i++) {
        result.push(declarations(css, ".g" + i)["--l"]);
    }
    return result;
}

// ---------------------------------------------------------------------------------------------------------------------
// Level 2: the full theme compiled with a token set applied
// ---------------------------------------------------------------------------------------------------------------------

const ROLES = ["primary", "secondary", "success", "info", "warning", "danger"];
// the radius scale ratios of the Java side (ThemeTokenCss): small 0.75, large 1.5, extra large 2 times the base radius
const RADIUS_RATIOS = { "border-radius-sm": 0.75, "border-radius-lg": 1.5, "border-radius-xl": 2 };

const CASES = [
    { name: "primary-default", tokens: { primary: "#4f46e5" } },
    { name: "primary-teal", tokens: { primary: "#0f766e" } },
    { name: "primary-black", tokens: { primary: "#000000" } },
    { name: "primary-white", tokens: { primary: "#ffffff" } },
    { name: "primary-yellow-flips-contrast", tokens: { primary: "#ffe066" } },
    { name: "primary-mid-grey", tokens: { primary: "#808080" } },
    { name: "primary-pink", tokens: { primary: "#d63384" } },
    { name: "primary-near-black", tokens: { primary: "#0a0a0a" } },
    { name: "primary-near-white", tokens: { primary: "#fafafa" } },
    { name: "primary-short-hex", tokens: { primary: "#0f6" } },
    { name: "secondary-violet", tokens: { secondary: "#7c3aed" } },
    { name: "success-flips-contrast", tokens: { success: "#0e9f6e" } },
    { name: "info-blue", tokens: { info: "#0284c7" } },
    { name: "warning-amber", tokens: { warning: "#f59e0b" } },
    { name: "danger-crimson", tokens: { danger: "#be123c" } },
    { name: "danger-light-flips-contrast", tokens: { danger: "#fca5a5" } },
    { name: "radius-px", tokens: { borderRadius: "20px" } },
    { name: "radius-rem", tokens: { borderRadius: "0.5rem" } },
    { name: "font-family", tokens: { fontFamily: 'Foo, "Bar Baz", sans-serif' } },
    {
        name: "everything-at-once",
        tokens: {
            primary: "#0f766e", secondary: "#7c3aed", success: "#0e9f6e", info: "#0284c7", warning: "#f59e0b", danger: "#be123c",
            borderRadius: "12px", fontFamily: "Foo, sans-serif",
        },
    },
];

function formatNumber(value) {
    return String(Math.round(value * 10000) / 10000);
}

function scaledLength(length, ratio) {
    const match = /^([0-9.]+)(px|rem)$/.exec(length);
    return formatNumber(parseFloat(match[1]) * ratio) + match[2];
}

function compileTheme(tokens) {
    let source = themeScss;
    let extra = "";
    for (const role of ROLES) {
        if (tokens[role]) {
            if (role === "primary") {
                if (!source.includes("$primary: #4f46e5;")) {
                    throw new Error("react4j-modern.scss no longer defines '$primary: #4f46e5;' - adjust regenerate-golden.js");
                }
                source = source.replace("$primary: #4f46e5;", "$primary: " + tokens[role] + ";");
            } else {
                extra += "$" + role + ": " + tokens[role] + ";\n";
            }
        }
    }
    if (tokens.borderRadius) {
        extra += "$border-radius: " + tokens.borderRadius + ";\n";
        for (const [variable, ratio] of Object.entries(RADIUS_RATIOS)) {
            extra += "$" + variable + ": " + scaledLength(tokens.borderRadius, ratio) + ";\n";
        }
    }
    if (tokens.fontFamily) {
        extra += "$font-family-base: " + tokens.fontFamily + ";\n";
    }
    const marker = '@import "bootstrap/scss/bootstrap";';
    if (!source.includes(marker)) {
        throw new Error("react4j-modern.scss no longer ends in the bootstrap import - adjust regenerate-golden.js");
    }
    source = source.replace(marker, extra + marker);
    return sass.compileString(source, {
        loadPaths: [path.join(reactDir, "node_modules")],
        style: "expanded",
        quietDeps: true,
        logger: sass.Logger.silent,
        silenceDeprecations: ["import"],
    }).css;
}

function normaliseSelector(selector) {
    return selector.replace(/\s*,\s*/g, ",").replace(/\s+/g, " ").trim();
}

/** selector -> prop -> {value, important}, last declaration wins; fails on rules inside at-rules (the renderer emits none) */
function flatten(css) {
    const map = new Map();
    postcss.parse(css).walkRules((rule) => {
        if (rule.parent && rule.parent.type === "atrule") {
            return;
        }
        const selector = normaliseSelector(rule.selector);
        if (!map.has(selector)) {
            map.set(selector, new Map());
        }
        rule.each((node) => {
            if (node.type === "decl") {
                map.get(selector).set(node.prop, { value: node.value.replace(/\s+/g, " "), important: !!node.important });
            }
        });
    });
    return map;
}

function main() {
    const functionGolden = {
        sassVersion,
        bootstrapVersion,
        colors: FUNCTION_COLORS.map(functionCase),
        greyLuminance: greyLuminance(),
    };
    fs.writeFileSync(path.join(__dirname, "bootstrap-function-golden.json"), JSON.stringify(functionGolden, null, 1) + "\n", "utf8");

    const base = flatten(compileTheme({}));
    const cases = CASES.map((testCase) => {
        const compiled = flatten(compileTheme(testCase.tokens));
        const differing = [];
        for (const [selector, properties] of compiled) {
            for (const [property, declaration] of properties) {
                const before = base.has(selector) ? base.get(selector).get(property) : undefined;
                if (!before || before.value !== declaration.value || before.important !== declaration.important) {
                    differing.push({ selector, property, value: declaration.value, important: declaration.important });
                }
            }
        }
        return { name: testCase.name, tokens: testCase.tokens, differing };
    });
    fs.writeFileSync(path.join(__dirname, "theme-sheet-golden.json"), JSON.stringify({ sassVersion, bootstrapVersion, cases }, null, 1) + "\n", "utf8");

    console.log("golden: " + functionGolden.colors.length + " function colours, " + cases.length + " theme cases, " +
        cases.reduce((sum, c) => sum + c.differing.length, 0) + " differing declarations");
}

main();
